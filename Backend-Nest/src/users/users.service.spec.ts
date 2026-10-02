import { User } from './user.entity';
import { UsersService } from './users.service';

const service = new UsersService({} as any);

function user(overrides: Partial<User>): User {
  return {
    id: 1,
    email: 'owner@example.com',
    fullName: 'Owner',
    userName: 'owner',
    phoneNumber: '+33600000000',
    bio: 'about me',
    profilePrivacy: 'public',
    emailPrivacy: 'public',
    phonePrivacy: 'public',
    friendIds: [],
    googleId: 'google-123',
    ...overrides,
  } as User;
}

describe('UsersService.toPublicProfile privacy', () => {
  it('shows everything that is public', () => {
    const view = service.toPublicProfile(user({}), 2);
    expect(view).toMatchObject({ email: 'owner@example.com', phoneNumber: '+33600000000', bio: 'about me' });
  });

  it('hides private fields from everyone else', () => {
    const view = service.toPublicProfile(
      user({ profilePrivacy: 'private', emailPrivacy: 'private', phonePrivacy: 'private' }),
      2,
    );
    expect(view).toMatchObject({ email: null, phoneNumber: null });
    expect(view).not.toHaveProperty('bio');
  });

  it('shows friends-only fields to listed friends only', () => {
    const owner = user({ profilePrivacy: 'friends', emailPrivacy: 'friends', friendIds: [2] });
    expect(service.toPublicProfile(owner, 2)).toMatchObject({ email: 'owner@example.com', bio: 'about me', isFriend: true });
    expect(service.toPublicProfile(owner, 3)).toMatchObject({ email: null, isFriend: false });
    expect(service.toPublicProfile(owner, 3)).not.toHaveProperty('bio');
  });

  it('never exposes account internals to other users', () => {
    expect(service.toPublicProfile(user({}), 2)).not.toHaveProperty('googleId');
  });

  it('returns the full profile to the owner', () => {
    const owner = user({ profilePrivacy: 'private', emailPrivacy: 'private' });
    expect(service.toPublicProfile(owner, 1)).toMatchObject({ email: 'owner@example.com', googleId: 'google-123' });
  });
});
