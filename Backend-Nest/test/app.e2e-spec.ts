import { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import { ThrottlerGuard } from '@nestjs/throttler';
import * as request from 'supertest';
import { AppModule } from '../src/app.module';
import { configureApp } from '../src/configure-app';
import { MailService } from '../src/mail/mail.service';
import { JamendoService } from '../src/music/jamendo.service';

// Real app + real Postgres (musicroom_test, see test/env.ts). Only the outside world is
// faked: outgoing mail is captured, and Jamendo returns tracks in *reverse* order so the
// playlist-order test proves the server re-sorts them.
const mails = new Map<string, string>();
const fakeJamendo = {
  tracksByIds: async (ids: string[]) => ({ results: [...ids].reverse().map((id) => ({ id, name: `Track ${id}` })) }),
  trackSearch: async () => ({ results: [] }),
  randomSongs: async () => ({ results: [] }),
  relatedForGenres: async () => ({ results: [] }),
  popularArtists: async () => ({ results: [] }),
};

const run = Date.now().toString(36);
const PASSWORD = 'Passw0rd!123';
let app: INestApplication;
let http: ReturnType<typeof request>;

type Session = { id: number; userName: string; email: string; access: string; refresh: string };

async function signup(name: string): Promise<Session> {
  const email = `${name}.${run}@test.local`;
  const userName = `${name}_${run}`;
  await http.post('/api/users/create').send({ full_name: name, username: userName, email, password: PASSWORD }).expect(201);

  // Unverified accounts can't log in.
  await http.post('/api/users/login').send({ email, password: PASSWORD }).expect(401);
  const token = mails.get(email)!.match(/token=(\S+)/)![1];
  await http.get('/api/users/verify-email').query({ token }).expect(200);

  const res = await http.post('/api/users/login').send({ email, password: PASSWORD }).expect(200);
  return { id: res.body.user.id, userName, email, access: res.body.tokens.access, refresh: res.body.tokens.refresh };
}

const auth = (s: Session) => ({ Authorization: `Bearer ${s.access}` });

const VISA = { number: '4242 4242 4242 4242', expMonth: 12, expYear: 2099, cvc: '123', holderName: 'Test' };
const goPremium = (s: Session) =>
  http.post('/api/users/subscribe').set(auth(s)).send({ subscriptionType: 'premium', card: VISA }).expect(201);

beforeAll(async () => {
  const moduleRef = await Test.createTestingModule({ imports: [AppModule] })
    .overrideProvider(MailService)
    .useValue({ send: async (to: string, _subject: string, text: string) => void mails.set(to, text) })
    .overrideProvider(JamendoService)
    .useValue(fakeJamendo)
    // Auth throttling would 429 the many logins below; it's covered by @nestjs/throttler itself.
    .overrideGuard(ThrottlerGuard)
    .useValue({ canActivate: () => true })
    .compile();

  app = moduleRef.createNestApplication();
  configureApp(app);
  await app.listen(0); // one real listener - supertest would otherwise spin one up per request
  http = request(app.getHttpServer());
});

afterAll(() => app?.close());

describe('auth & tokens', () => {
  it('refresh swaps a refresh token for a new pair; access tokens are rejected there', async () => {
    const u = await signup('refresher');
    const res = await http.post('/api/users/token/refresh').send({ refresh_token: u.refresh }).expect(200);
    expect(res.body.tokens.access).toBeTruthy();
    await http.post('/api/users/token/refresh').send({ refresh_token: u.access }).expect(401);
  });

  it('logout (Android style: refresh_token only) revokes every issued token', async () => {
    const u = await signup('leaver');
    await http.get('/api/users/profile').set(auth(u)).expect(200);
    await http.post('/api/users/logout').send({ refresh_token: u.refresh }).expect(200);
    await http.get('/api/users/profile').set(auth(u)).expect(401);
    await http.post('/api/users/token/refresh').send({ refresh_token: u.refresh }).expect(401);
  });
});

describe('profile', () => {
  it('accepts a cleared date of birth (web form sends "") instead of a 500', async () => {
    const u = await signup('editor');
    await http.put('/api/users/profile/update').set(auth(u)).send({ date_of_birth: '1990-01-02' }).expect(200);
    const res = await http.put('/api/users/profile/update').set(auth(u)).send({ full_name: 'New Name', date_of_birth: '' }).expect(200);
    expect(res.body).toMatchObject({ fullName: 'New Name', dateOfBirth: null });
  });

  it('keeps fields the request leaves out', async () => {
    const u = await signup('keeper');
    await http.put('/api/users/profile/update').set(auth(u)).send({ date_of_birth: '1990-01-02' }).expect(200);
    const res = await http.put('/api/users/profile/update').set(auth(u)).send({ bio: 'hi' }).expect(200);
    expect(res.body.dateOfBirth).toBe('1990-01-02');
  });

  it('rejects invalid values with 400', async () => {
    const u = await signup('invalid');
    await http.put('/api/users/profile/update').set(auth(u)).send({ date_of_birth: 'nope' }).expect(400);
    await http.put('/api/users/profile/update').set(auth(u)).send({ email_privacy: 'everyone' }).expect(400);
  });

  it('enforces privacy levels, with "friends" granted through the friends list', async () => {
    const owner = await signup('private');
    const viewer = await signup('viewer');
    await http
      .put('/api/users/profile/update')
      .set(auth(owner))
      .send({ profile_privacy: 'friends', email_privacy: 'private', bio: 'secret' })
      .expect(200);

    let seen = await http.get(`/api/users/${owner.userName}`).set(auth(viewer)).expect(200);
    expect(seen.body).toMatchObject({ email: null });
    expect(seen.body).not.toHaveProperty('bio');

    await http.post('/api/users/friends/add').set(auth(owner)).send({ userName: viewer.userName }).expect(201);
    seen = await http.get(`/api/users/${owner.userName}`).set(auth(viewer)).expect(200);
    expect(seen.body).toMatchObject({ bio: 'secret', email: null, isFriend: true });
  });
});

describe('playlists', () => {
  it('hides private playlists from strangers and anonymous callers', async () => {
    const owner = await signup('plowner');
    const stranger = await signup('stranger');
    await goPremium(owner);
    const { body } = await http.post('/api/playlists/create').set(auth(owner)).send({ name: 'Secret', isPublic: false }).expect(201);

    await http.get(`/api/playlists/${body.id}`).set(auth(stranger)).expect(403);
    await http.get(`/api/playlists/${body.id}/tracks`).set(auth(stranger)).expect(403);
    await http.get(`/api/playlists/${body.id}/tracks`).expect(403);
    await http.get(`/api/playlists/${body.id}/tracks`).set(auth(owner)).expect(200);
  });

  it('returns tracks in playlist order, including after a reorder', async () => {
    const owner = await signup('orderer');
    const { body } = await http.post('/api/playlists/create').set(auth(owner)).send({ name: 'Ordered' }).expect(201);
    for (const t of ['1', '2', '3']) await http.post(`/api/playlists/${body.id}/tracks/${t}/add`).set(auth(owner)).expect(201);

    let res = await http.get(`/api/playlists/${body.id}/tracks`).set(auth(owner)).expect(200);
    expect(res.body.tracks.map((t: any) => t.id)).toEqual(['1', '2', '3']);

    await http.post(`/api/playlists/${body.id}/tracks/reorder`).set(auth(owner)).send({ trackOrder: ['3', '1', '2'] }).expect(201);
    res = await http.get(`/api/playlists/${body.id}/tracks`).set(auth(owner)).expect(200);
    expect(res.body.tracks.map((t: any) => t.id)).toEqual(['3', '1', '2']);
  });

  it('keeps every track when many are added concurrently (no lost updates)', async () => {
    const owner = await signup('burst');
    const { body } = await http.post('/api/playlists/create').set(auth(owner)).send({ name: 'Burst' }).expect(201);
    const ids = Array.from({ length: 12 }, (_, i) => String(100 + i));
    await Promise.all(ids.map((t) => http.post(`/api/playlists/${body.id}/tracks/${t}/add`).set(auth(owner)).expect(201)));

    const detail = await http.get(`/api/playlists/${body.id}`).set(auth(owner)).expect(200);
    expect([...detail.body.tracks].sort()).toEqual(ids);
  });
});

describe('events (Music Track Vote)', () => {
  it('counts every vote and every join under concurrency (no lost updates)', async () => {
    const organizer = await signup('organizer');
    const { body: event } = await http
      .post('/api/events/create')
      .set(auth(organizer))
      .send({ title: 'Race', location: 'E1', eventStartTime: '2030-01-01T20:00:00Z', isPublic: true })
      .expect(201);
    await http.post(`/api/events/${event.id}/tracks/42/add`).set(auth(organizer)).expect(201);

    const voters = await Promise.all(Array.from({ length: 12 }, (_, i) => signup(`voter${i}`)));
    await Promise.all(voters.map((v) => http.post(`/api/events/${event.id}/join`).set(auth(v)).expect(201)));
    await Promise.all(voters.map((v) => http.post(`/api/events/${event.id}/tracks/42/vote`).set(auth(v)).expect(201)));

    const detail = await http.get(`/api/events/${event.id}`).set(auth(organizer)).expect(200);
    expect(detail.body.attendeeCount).toBe(voters.length);
    expect(Object.keys(detail.body.userRoles)).toHaveLength(voters.length + 1);
    expect(detail.body.songs.find((s: any) => s.trackId === '42').voteCount).toBe(voters.length);
  });

  it('enforces the invited_only vote license', async () => {
    const organizer = await signup('licenser');
    const outsider = await signup('outsider');
    const { body: event } = await http
      .post('/api/events/create')
      .set(auth(organizer))
      .send({ title: 'Invite only', location: 'E2', eventStartTime: '2030-01-01T20:00:00Z' })
      .expect(201);
    await http.post(`/api/events/${event.id}/tracks/7/add`).set(auth(organizer)).expect(201);
    await http.put(`/api/events/${event.id}/vote-license`).set(auth(organizer)).send({ voteLicense: 'invited_only' }).expect(200);

    await http.post(`/api/events/${event.id}/tracks/7/vote`).set(auth(outsider)).expect(403);
    await http.post(`/api/events/${event.id}/join`).set(auth(outsider)).expect(201);
    await http.post(`/api/events/${event.id}/tracks/7/vote`).set(auth(outsider)).expect(201);
  });

  it('hides private events from non-attendees', async () => {
    const organizer = await signup('hider');
    const stranger = await signup('peeker');
    await goPremium(organizer);
    const { body: event } = await http
      .post('/api/events/create')
      .set(auth(organizer))
      .send({ title: 'Private', location: 'P1', eventStartTime: '2030-01-01T20:00:00Z', isPublic: false })
      .expect(201);
    await http.get(`/api/events/${event.id}`).set(auth(stranger)).expect(403);
    await http.get(`/api/events/${event.id}/tracks`).set(auth(stranger)).expect(403);
  });
});

describe('premium (simulated checkout)', () => {
  it('requires a valid card to upgrade, and stores only brand + last 4', async () => {
    const u = await signup('buyer');
    const sub = (body: object) => http.post('/api/users/subscribe').set(auth(u)).send(body);

    await sub({ subscriptionType: 'premium' }).expect(400);
    await sub({ subscriptionType: 'premium', card: { ...VISA, number: '4242 4242 4242 4241' } }).expect(400);
    await sub({ subscriptionType: 'premium', card: { ...VISA, expYear: 2020 } }).expect(400);
    await sub({ subscriptionType: 'premium', card: { ...VISA, number: '4000 0000 0000 0002' } }).expect(402);
    expect((await http.get('/api/users/profile').set(auth(u))).body.isPremium).toBe(false);

    const res = await sub({ subscriptionType: 'premium', card: VISA }).expect(201);
    expect(res.body).toMatchObject({ isPremium: true, card: { brand: 'Visa', last4: '4242' } });
    expect(res.body.premiumSince).toBeTruthy();
    expect(JSON.stringify(res.body)).not.toContain('4242424242424242');

    const down = await sub({ subscriptionType: 'free' }).expect(201);
    expect(down.body).toMatchObject({ isPremium: false, card: null, premiumSince: null });
  });

  it('free users can only create public playlists and events, and cannot switch them to private', async () => {
    const u = await signup('freebie');
    await http.post('/api/playlists/create').set(auth(u)).send({ name: 'p', isPublic: false }).expect(403);
    const event = { title: 'e', location: 'E1', eventStartTime: '2030-01-01T20:00:00Z' };
    await http.post('/api/events/create').set(auth(u)).send({ ...event, isPublic: false }).expect(403);

    const { body: pl } = await http.post('/api/playlists/create').set(auth(u)).send({ name: 'p' }).expect(201);
    await http.put('/api/playlists/' + pl.id + '/visibility').set(auth(u)).send({ isPublic: false }).expect(403);
    await http.put('/api/playlists/' + pl.id + '/update').set(auth(u)).send({ isPublic: false }).expect(403);
    const { body: ev } = await http.post('/api/events/create').set(auth(u)).send(event).expect(201);
    await http.put('/api/events/' + ev.id + '/visibility').set(auth(u)).send({ isPublic: false }).expect(403);

    await goPremium(u);
    await http.put('/api/playlists/' + pl.id + '/visibility').set(auth(u)).send({ isPublic: false }).expect(200);
    await http.put('/api/events/' + ev.id + '/visibility').set(auth(u)).send({ isPublic: false }).expect(200);
  });

  it('free accounts are capped at 3 playlists and 3 events; premium lifts the cap', async () => {
    const u = await signup('capped');
    const event = { title: 'e', location: 'E1', eventStartTime: '2030-01-01T20:00:00Z' };
    for (let i = 0; i < 3; i++) {
      await http.post('/api/playlists/create').set(auth(u)).send({ name: 'p' + i }).expect(201);
      await http.post('/api/events/create').set(auth(u)).send(event).expect(201);
    }
    await http.post('/api/playlists/create').set(auth(u)).send({ name: 'p4' }).expect(403);
    await http.post('/api/events/create').set(auth(u)).send(event).expect(403);

    await goPremium(u);
    await http.post('/api/playlists/create').set(auth(u)).send({ name: 'p4' }).expect(201);
    await http.post('/api/events/create').set(auth(u)).send(event).expect(201);
  });
});
