# Music Room — Web

Next.js (App Router) web client for Music Room. Spotify-style layout (sidebar, top bar, fixed
bottom now-playing bar), themed with the mobile app's color palette (see
`App/app/src/main/java/com/example/musicroom/presentation/theme/AppColors.kt`, mirrored into
`app/globals.css`). Full feature parity with the mobile app — see the root `PROJECT_STATUS.md`
Bonus section for the audit history.

## Setup

Settings come from the root `.env` of the repo (`NEXT_PUBLIC_*` keys only, loaded by
`next.config.ts`); there is no env file in this folder. From the repo root: `make web`, or the whole
stack with `make dev` / `make up` (see the root README).

### Google Sign-In

Needs a Google **Web** OAuth client ID (Google Cloud Console → Credentials → Create OAuth client
ID → type "Web application", with `http://localhost:3000` as an authorized JavaScript origin —
this is *not* the Android client ID already in `App/app/google-services.json`, that one is scoped
to the Android app and won't work here). Set it as `NEXT_PUBLIC_GOOGLE_CLIENT_ID` in the root `.env`.
Without it, the Google button renders with an inline "not configured" note instead of failing
silently.

## Structure

- `app/(auth)/` — login, register, forgot-password. Public.
- `app/(app)/` — authenticated shell (`layout.tsx` does the client-side auth-guard redirect) plus
  home, search, playlists (discovery + `[id]` detail), events (list/create + `[id]` detail),
  artists/`[id]`, devices, profile.
- `lib/api.ts` — thin fetch wrapper around the NestJS API, attaches the JWT from `localStorage`.
- `lib/auth-context.tsx`, `lib/player-context.tsx` — global auth state and the audio player
  (single `<audio>` element, drives the bottom bar).
- `components/` — Sidebar, Topbar, NowPlayingBar, NotificationBell, EventManagePanel,
  AddToPlaylistButton/AddToEventButton (portal-based dropdowns — see the comment in
  `AddToPlaylistButton.tsx` for why they're portaled to `document.body` rather than
  absolutely-positioned children), and the card/row components used across pages.

## Notable implementation details

- **Real-time playlist collab**: `app/(app)/playlists/[id]/page.tsx` opens a `socket.io-client`
  connection to the existing `playlists.gateway.ts` WebSocket gateway alongside the normal REST
  flow, and reloads on `track_added`/`track_removed`/`tracks_reordered` broadcasts from other
  users viewing the same playlist. Additive, not a replacement for the REST calls.
- **Device delegation**: a stable per-browser device id is generated once and cached in
  `localStorage` (`musicroom_device_id`), then registered via `POST /devices/register`.
- **Artist pages**: `GET /music/artists/:id` (added to the backend alongside this) resolves a
  Jamendo artist by id and its real tracks (`id=`/`artist_id=` filters) - not an approximation.
- **Playlist visibility**: invites only work on private playlists (backend-enforced), so the
  playlist page has a visibility toggle (`PUT /playlists/:id/visibility`) and the sidebar's create
  flow asks public-or-private up front.

## Explicitly out of scope

- SSR/middleware auth guarding, refresh-token rotation, "remember me" — JWT in `localStorage`,
  client-side redirect only.
