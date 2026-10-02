# Music Room — Status vs. 42 Subject (v6)

Audited 2026-09-21, updated 2026-09-29 against `en.subject.pdf`. Backend = `Backend-Nest` (NestJS +
TypeORM + Postgres).

Legend: ✅ Done · 🟡 Partial · ❌ Missing

## Still missing (blocks "mandatory = perfect")

Nothing on the mandatory list. Everything that was here was closed on 2026-09-29 (see below).
Remaining soft spots, none of which the subject requires:
- Android changes from 2026-09-29 (device headers on Home/Music calls, NetworkConfigTest) could not
  be compiled on the audit machine (no JDK) - run `make app-build app-test` once to confirm.
- The mobile app has no Friends screen yet (friends can be managed from the web profile page).
- Facebook login: not implemented; the subject asks for Facebook **or** Google, and Google is done.

## Fixed on 2026-09-29

1. **✅ Automated tests** - Jest in `Backend-Nest`: unit tests (`jamendo.service.spec.ts` cache/retry,
   `users.service.spec.ts` privacy rules) + an e2e suite (`test/app.e2e-spec.ts`, 12 tests) against a
   real `musicroom_test` Postgres DB covering auth/verification, token refresh + revocation, profile
   update/validation, privacy + friends, playlist/event visibility, playlist ordering, vote licenses and
   concurrency. Web: `web/lib/api.test.ts` (`node --test`). Android: `NetworkConfigTest.kt` replaces the
   generated placeholder. Run everything with `make test`.
2. **✅ Concurrency race (V.2)** - every write in `events.service.ts` and `playlists.service.ts` now goes
   through `mutate()`: a transaction that takes a `SELECT ... FOR UPDATE` row lock before the
   load -> mutate -> save. The e2e tests fire 12 concurrent joins/votes/adds; with the lock removed
   they fail (10/12 votes, 2/12 tracks kept), with it they pass. WebSocket broadcasts now fire after
   commit, so listeners never refetch stale data.
3. **✅ CI** - `.github/workflows/ci.yml`: backend (typecheck + unit + e2e with a Postgres service),
   web (lint, typecheck, tests, production build), Android unit tests (enabled by the
   `GOOGLE_SERVICES_JSON` secret).
4. **✅ Privacy enforced (V.1)** - `GET /users/:userName` goes through `UsersService.toPublicProfile()`:
   `profilePrivacy` gates bio/birth date/music taste, `emailPrivacy`/`phonePrivacy` gate those fields,
   account internals are never shown. `GET /users` hides e-mails the same way. "Friends" = users the
   owner lists via `/users/friends/*` (managed from the web profile page).
5. **✅ Token theft mitigation (V.6)** - JWTs carry the user's `tokenVersion`; `POST /users/logout`
   bumps it, revoking every access and refresh token issued so far (works with a bearer token or the
   Android `{ refresh_token }` body). `POST /users/token/refresh` issues a new pair; the web client
   refreshes automatically on 401 instead of logging out every hour.
6. **✅ Other fixes** - profile save 500 on an empty birth date; Redis caching Jamendo's spurious
   empty responses for an hour (tracks vanishing from events/playlists); private playlists readable by
   anyone; playlist reorder not reflected; malformed verification link when DEFAULT_API_URL has a
   trailing slash; `make` targets pointing at a retired service (now: `make install / dev /
   test`, backend runs on the host, Postgres/Redis in Docker); device headers missing on the Android
   Home/Music/player calls.

## Fixed since the 2026-09-21 audit

1. **✅ Security leak** — `GET /users/:userName` (`users.controller.ts`) now routes through
   `UsersService.toProfile()` instead of returning the raw `User` entity, so the bcrypt `password`
   hash and `passwordResetOtp` no longer leak to other authenticated users. Also removed 3 debug
   `console.log`s that were printing full user entities (password hash included) to server logs.
2. **✅ Brute-force protection** — `@nestjs/throttler` added; `AuthController` runs under
   `ThrottlerGuard` (`@UseGuards`, not global — see V.7 note on why) with `login`,
   `resend-verification-email`, `password-reset*`, and `social-login` limited to 5 req/min/IP.
   Verified: 6th rapid `/users/login` attempt returns `429` with `Retry-After`.
3. **✅ Ramp-up evidence** — see V.7 below: JMeter test plan in
   `Backend-Nest/test/performance/jmeter/`, measured against all 3 services, numbers and box specs
   written up. Also caught a real instance of the V.2 concurrency race (item 2 above) under actual
   load, not just a theoretical concern.

## V.1 — User

| Requirement | Status | Evidence |
|---|---|---|
| Email/password signup | ✅ | `auth.service.ts:33-53`, bcrypt hash, `isActive=false` until verified |
| Social (Google) signup/login | ✅ | `auth.service.ts:147-178` |
| Facebook signup/login | ❌ | Not implemented anywhere (spec says "Facebook **or** Google" — Google alone satisfies the letter of it, but only one provider exists) |
| Link social account to existing account | ✅ | `auth.service.ts:180-191`, `social-link` endpoint |
| Email verification after mail/password signup | ✅ | `auth.service.ts:64-87`, JWT-signed link + resend endpoint |
| Forgot/reset password | ✅ | `auth.service.ts:100-145`, OTP flow (request → verify-otp → confirm), mobile `ForgotPasswordScreen.kt` |
| Public / friends-only / private profile fields | ✅ | `users.service.ts` `toPublicProfile()` enforces `profilePrivacy`/`emailPrivacy`/`phonePrivacy`; "friends" = ids in `User.friendIds`, managed via `/users/friends/*`. Covered by `users.service.spec.ts` + e2e. |

## V.2 — Services (spec needs 2 of 3 working; all 3 exist here)

### Music Track Vote — ✅ working
- Visibility (`isPublic`) and vote license (`everyone` / `invited_only` / `time_window`) both
  implemented — `event.entity.ts`, `events.service.ts:255-277`.
- ✅ **Concurrency**: all writes run in `mutate()` (transaction + `SELECT ... FOR UPDATE`); proven by the concurrent vote/join e2e test.

### Music Control Delegation — ✅ working, matches spec closely
- Per-device delegation as required: `device.entity.ts`, `control-delegate.entity.ts` (unique
  per device+delegate), `devices.service.ts` (delegate / revoke / sendCommand /
  consumePendingCommands). Mobile side: `DeviceControlScreen.kt`, `DeviceControlViewModel.kt`.

### Music Playlist Editor — ✅ working, real-time
- Actual WebSocket gateway (`playlists.gateway.ts`, JWT-authenticated room join), not polling.
- Visibility (`isPublic`) + license (`couldEdit` list) implemented.
- ✅ Same `mutate()` row lock as the vote service; concurrent-add e2e test.

## V.3 Server / V.4 API — ✅ done
- Real persistent store: Postgres via TypeORM (`app.module.ts:24-36`), not an in-memory or
  file-based mock.
- Swagger wired at `/swagger` with bearer-auth support (`main.ts:15-24`) — documents inputs and
  outputs as required.
- REST + JSON throughout — matches the spec's endorsed (but not mandatory) defaults.

## V.5 — Mobile application
- ✅ Backend base URL is runtime-configurable (`ServerSettingsScreen.kt` + `NetworkConfig.kt`) —
  satisfies "configurable on the application for tests."
- ✅ Social auth implemented for Google (`GoogleAuthUiClient.kt`) — spec asks for "Facebook or
  Google," so this alone satisfies it. No Facebook SDK integration, not needed.
- App is otherwise a thin remote control to the API, per spec.

## V.6 — Securing
- ✅ Global JWT guard on all routes (`app.module.ts:51`, `jwt-auth.guard.ts`) with an explicit
  `@Public()` opt-out per route.
- ✅ Brute-force/throttling protection — `@nestjs/throttler`, `ThrottlerGuard` scoped to
  `AuthController` (`@UseGuards`, not `APP_GUARD`). `login` / `resend-verification-email` /
  `password-reset*` / `social-login` capped at 5 req/min/IP; other `AuthController` routes get
  the module default (60 req/min/IP). Deliberately **not** global: the first attempt registered
  `ThrottlerGuard` as an `APP_GUARD`, and the V.7 load test immediately proved why that's wrong —
  a global per-IP limit throttles ordinary traffic from any shared IP (NAT, mobile carrier, a load
  test client) exactly like it throttles a real attacker. Scoped down to just the endpoints the
  spec calls out.
- ✅ Activity logging captures Platform / Device / App Version per action —
  `activity-log.middleware.ts` reads `X-Platform` / `X-Device-Model` / `X-App-Version` headers
  into `action_logs`; mobile actually sends them (`NetworkConfig.kt:83-85`). This is a
  genuinely solid piece of the mandatory spec.
- ✅ `.env` is gitignored; `.env.example` documents every required key at repo root.
- ✅ Profile leak (see top of file) is fixed — this was the actual authorization gap the spec is
  testing for ("access to their data, but not to other users' data").

## V.7 — Ramp-up — ✅ done

**Tooling**: Apache JMeter 5.6.3 + Temurin 21 JRE (installed via `winget`, not committed to the
repo — see `test/performance/jmeter/README.md` for install/run instructions). Test plan:
`Backend-Nest/test/performance/jmeter/musicroom-rampup.jmx`. Fixture/token setup:
`Backend-Nest/test/performance/jmeter/setup-fixtures.sh` (creates 5 throwaway accounts, an
event+track, a delegated device, and a playlist with edit permissions). The README in that folder
walks through the test plan's structure (Thread Groups, the Counter Config gotcha, etc.) and the
gotchas that came up while building it (login throttle, token expiry).

**Box the server ran on** (client and server co-located on the same machine — see caveat below):
Intel Core i7-8650U (4 cores / 8 threads) @ 1.90GHz, 15.8 GB RAM (only ~1.3 GB free at test time —
this box was already under memory pressure from other work), Windows 11 Pro x64. Single Nest
process (`npm run start:dev`, no clustering), single Postgres 15 container, no Redis caching in
front of the DB reads/writes exercised here.

**Method**: ramped concurrent threads per Thread Group (10 → 25 → 50 → 100 → 200 — all 3 Thread
Groups use the same count, so e.g. "50" means 150 total concurrent virtual users across the 3
services), 15 loops/thread, each thread alternating add/remove (or vote/unvote) against a shared
fixture so the request mix hits the real read-modify-write path on the `events`/`playlists` jsonb
columns flagged as a race risk in V.2:

```
jmeter -n -t musicroom-rampup.jmx -Jthreads=N -Jrampup=S -Jloops=15 -l results/level-N.jtl
```

| Threads/service | Total reqs (3 services) | Throughput | Avg latency | p95 latency | Max latency | Errors |
|---:|---:|---:|---:|---:|---:|---:|
| 10  | 900    | 141 req/s | 156ms   | 266ms   | 316ms   | 0% |
| 25  | 2,250  | 148 req/s | 430ms   | 809ms   | 1,152ms | 0% |
| 50  | 4,500  | 202 req/s | 576ms   | 1,036ms | 1,237ms | 0% |
| 100 | 9,000  | 158 req/s | 1,528ms | 2,687ms | 3,063ms | 0% |
| 200 | 18,000 | 153 req/s | 3,335ms | 5,627ms | 6,954ms | 0% |

**Reading it**: zero HTTP errors at every level up to 200 threads/service (600 total concurrent
users) — this app does not crash or reject requests under load, it queues. Throughput plateaus at
~140-200 req/s from 25 threads/service onward and never goes much higher no matter how many more
threads pile on; latency instead grows roughly linearly with concurrency (p95 266ms → 5.6s). That's
the signature of a single fixed-capacity bottleneck (single Node event loop, one Postgres connection
pool, no clustering) queueing requests rather than a per-request cost problem. **Practical capacity
for this box**: comfortable up to ~25-50 threads/service (sub-second p95); beyond ~100 the app is
technically "up" but unusably slow (p95 > 2s) for a real-time voting/playlist app.

**Bonus finding — the V.2 concurrency race is real, not theoretical**: running the Playlist Editor
scenario left the fixture playlist with hundreds of stray tracks (235-306 depending on the run)
even though every individual add/remove request returned 200/201. Root cause confirmed in
`playlists.service.ts` `addTrack`/`removeTrack` (same shape in `events.service.ts`
`voteTrack`/`unvoteTrack`): both do `load → mutate in memory → save` on the `tracks`/`trackVotes`
jsonb column with no transaction or optimistic lock, so two concurrent requests on the same row can
each read the same stale state and the second save silently discards the first writer's change —
a request-level "add" or "remove" reports success but its effect on the row is lost. This is a
genuine lost-update bug under real concurrency, not just a load-test artifact — confirms the V.2
recommendation (`SELECT ... FOR UPDATE` or a version column) is worth doing before defense, not
just a nice-to-have. **Fixed 2026-09-29** (row lock in `mutate()`, regression-tested in the e2e suite); at the time, the fixture
playlist was reset (`tracks` back to `["jamendo-track-1"]`) after each run so `results/`-free reruns
start clean.

**Caveats**: (1) client and server shared the same CPU/RAM here, so these numbers are a *relative*
capacity curve, not an absolute one — a dedicated test client would likely show a lower ceiling on
the server alone. (2) All requests came from one IP, which is exactly what caught the
`ThrottlerGuard`-as-`APP_GUARD` bug above — worth remembering if this test plan is ever pointed at
a deployed instance that still has a global rate limiter. (3) Reruns should regenerate fixtures via
`setup-fixtures.sh` rather than reusing `tokens.csv` (gitignored, JWTs expire in 1h) — and reset the
playlist fixture's `tracks` column first if a previous run left it polluted by the race above.

## V.8 — Agility, quality, CI — ✅ done
Tests for each layer (backend unit + e2e, web, Android JVM) and a GitHub Actions workflow running
them on every push - see "Fixed on 2026-09-29" at the top and the Tests section of `README.md`.

## Bonus (only graded once mandatory is perfect — noted briefly)
- ✅ **Web client (done 2026-09-23, brought to full mobile parity 2026-09-28)** — `web/`
  (Next.js 16, App Router, TypeScript, Tailwind v4). Spotify-style layout (persistent sidebar, top
  bar, fixed bottom now-playing bar with a working `<audio>` player), themed with the mobile app's
  exact color palette. A mobile-vs-web feature audit on 2026-09-28 found ~11 gaps; all were closed:
  forgot/reset password, Google Sign-In (real GSI flow, needs a Web OAuth client ID the user
  supplies via `NEXT_PUBLIC_GOOGLE_CLIENT_ID` — not something this session can create), profile
  editing + privacy settings, public playlist discovery, playlist invites (+ visibility toggle,
  added because invites are blocked on public playlists and there was previously no way to make
  one private), event invites/role management/full vote-license options, add-to-playlist/event
  from the now-playing bar, an artist detail page, an in-app notification bell, device control
  delegation, and real-time WebSocket playlist collab (`socket.io-client`, live-refreshes on
  `track_added`/`track_removed`/`tracks_reordered`). See `web/README.md`.
  - **Found and fixed two more real backend bugs while verifying this round**: (1)
    `JamendoService.tracksByIds()` passed `include: 'musicinfo'` alongside the `id` filter —
    verified directly against Jamendo's API that this silently makes the `id` filter match
    nothing, so every playlist/event track list was returning empty regardless of content, in
    both the mobile and web app, since the endpoint existed. Fixed by dropping the param. (2) No
    endpoint existed to fetch a single artist's real tracks by id — `GET /music/artists/:id`
    added (`jamendo.service.ts` `artistById`/`tracksByArtistId`, verified Jamendo supports
    `id=`/`artist_id=` filters directly); the web artist page originally tried approximating this
    via name-search, which returned nothing for a numeric id and left the page stuck on
    "Loading…" forever (`setState(null)` on an already-`null` state doesn't re-render) - a bug
    caught by the same headless-browser pass, not by build/lint/typecheck.
  - Verified end-to-end with two real accounts across the whole feature set: playlist invite sent
    → shows in the invitee's notification bell → accept → both users see the same playlist update
    live via WebSocket with no manual refresh; device delegated → delegate sends a real playback
    command; profile edit persists; artist page loads real tracks.
- ❌ No IoT/iBeacon code.
- ✅ **Free vs Premium (reworked 2026-09-29)** — upgrading goes through a simulated checkout
  (`users/subscription.ts`): card validated like a real processor (Luhn, expiry, brand-specific CVC),
  `4000 0000 0000 0002` is declined with 402 (Stripe test-card convention), only brand + last 4 are
  stored, no money moves. Premium unlocks unlimited playlists/events (free: 3 each) and private
  playlists/events (create or switch to private), enforced server-side against the owner. Web: plan
  comparison + checkout form on the profile page, Premium-gated private options in create flows.
  Cancelling keeps existing content. Covered by `subscription.spec.ts` + 3 e2e tests. No renewal cycle
  (premium lasts until cancelled); Android shows the plan but has no checkout screen yet.
- ❌ No offline mode / sync mechanism.

The mandatory list is closed, so these are now worth picking up.
