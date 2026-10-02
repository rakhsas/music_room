import type {
  AuthTokens,
  Device,
  Friend,
  EventDetail,
  EventRolesResponse,
  EventSummary,
  EventTracksResponse,
  HomeResponse,
  JamendoArtist,
  JamendoListResponse,
  JamendoTrack,
  LoginResponse,
  MyEventSummary,
  OtherUser,
  PaymentCard,
  PendingEventInvite,
  PlaylistDetail,
  PlaylistSummary,
  PlaylistTracksResponse,
  UserProfile,
} from "./types";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8000/api";
// The playlists WebSocket gateway lives at the server root, not under /api.
export const WS_BASE_URL = BASE_URL.replace(/\/api\/?$/, "");
export const TOKEN_KEY = "musicroom_token";
const REFRESH_KEY = "musicroom_refresh";

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function saveTokens(tokens: AuthTokens) {
  localStorage.setItem(TOKEN_KEY, tokens.access);
  localStorage.setItem(REFRESH_KEY, tokens.refresh);
}

export function clearTokens() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_KEY);
}

// Access tokens live 60 min - swap the refresh token for a new pair instead of
// bouncing the user to the login page every hour.
async function tryRefresh(): Promise<boolean> {
  const refresh = typeof window === "undefined" ? null : localStorage.getItem(REFRESH_KEY);
  if (!refresh) return false;
  const res = await fetch(`${BASE_URL}/users/token/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refresh_token: refresh }),
  }).catch(() => null);
  if (!res?.ok) return false;
  saveTokens((await res.json()).tokens);
  return true;
}

async function request<T>(path: string, options: RequestInit = {}, retried = false): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...((options.headers as Record<string, string>) ?? {}),
  };
  if (token) headers.Authorization = `Bearer ${token}`;

  const res = await fetch(`${BASE_URL}${path}`, { ...options, headers });
  if (res.status === 401 && token && !retried && (await tryRefresh())) return request<T>(path, options, true);
  const isJson = res.headers.get("content-type")?.includes("application/json");
  const body = isJson ? await res.json().catch(() => null) : null;

  if (!res.ok) {
    const raw = body?.message ?? res.statusText ?? "Request failed";
    throw new ApiError(res.status, Array.isArray(raw) ? raw.join(", ") : raw);
  }
  return body as T;
}

export const api = {
  login: (email: string, password: string) =>
    request<LoginResponse>("/users/login", { method: "POST", body: JSON.stringify({ email, password }) }),

  register: (data: { full_name: string; username: string; email: string; password: string }) =>
    request<{ id: number; email: string; fullName: string; userName: string; message: string }>("/users/create", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  profile: () => request<UserProfile>("/users/profile"),
  logout: () => request<{ message: string }>("/users/logout", { method: "POST" }),

  friends: () => request<Friend[]>("/users/friends/list"),
  addFriend: (userName: string) =>
    request<{ message: string }>("/users/friends/add", { method: "POST", body: JSON.stringify({ userName }) }),
  removeFriend: (userId: number) => request<{ message: string }>(`/users/friends/${userId}`, { method: "DELETE" }),

  updateProfile: (data: Record<string, unknown>) =>
    request<UserProfile>("/users/profile/update", { method: "PUT", body: JSON.stringify(data) }),

  subscribe: (subscriptionType: "free" | "premium", card?: PaymentCard) =>
    request<UserProfile>("/users/subscribe", { method: "POST", body: JSON.stringify({ subscriptionType, card }) }),

  socialLogin: (provider: "google", accessToken: string) =>
    request<LoginResponse>("/users/social-login", {
      method: "POST",
      body: JSON.stringify({ provider, access_token: accessToken }),
    }),

  requestPasswordReset: (email: string) =>
    request<{ message: string }>("/users/password-reset", { method: "POST", body: JSON.stringify({ email }) }),
  verifyPasswordResetOtp: (email: string, otp: string) =>
    request<{ message: string }>("/users/password-reset-verify-otp", { method: "POST", body: JSON.stringify({ email, otp }) }),
  confirmPasswordReset: (email: string, otp: string, password: string) =>
    request<{ message: string }>("/users/password-reset-confirm", {
      method: "POST",
      body: JSON.stringify({ email, otp, password }),
    }),

  otherUsers: () => request<OtherUser[]>("/users"),

  home: () => request<HomeResponse>("/home"),

  searchTracks: (query: string) => request<JamendoListResponse<JamendoTrack>>(`/music/tracks/search-name/${encodeURIComponent(query)}`),
  searchArtists: (query: string) => request<JamendoListResponse<JamendoArtist>>(`/music/artists/search/${encodeURIComponent(query)}`),
  artistDetail: (id: string) => request<{ artist: JamendoArtist | null; tracks: JamendoTrack[] }>(`/music/artists/${encodeURIComponent(id)}`),

  publicPlaylists: () => request<PlaylistSummary[]>("/playlists"),
  myPlaylists: () => request<{ playlists: PlaylistSummary[]; count: number }>("/playlists/my"),
  playlist: (id: number | string) => request<PlaylistDetail>(`/playlists/${id}`),
  playlistTracks: (id: number | string) => request<PlaylistTracksResponse>(`/playlists/${id}/tracks`),
  createPlaylist: (name: string, isPublic = true) =>
    request<{ id: number; name: string; message: string }>("/playlists/create", {
      method: "POST",
      body: JSON.stringify({ name, isPublic }),
    }),
  addTrackToPlaylist: (playlistId: number | string, trackId: string) =>
    request(`/playlists/${playlistId}/tracks/${encodeURIComponent(trackId)}/add`, { method: "POST" }),
  removeTrackFromPlaylist: (playlistId: number | string, trackId: string) =>
    request(`/playlists/${playlistId}/tracks/${encodeURIComponent(trackId)}/remove`, { method: "DELETE" }),
  setPlaylistVisibility: (playlistId: number | string, isPublic: boolean) =>
    request<{ message: string }>(`/playlists/${playlistId}/visibility`, { method: "PUT", body: JSON.stringify({ isPublic }) }),
  invitePlaylist: (playlistId: number | string, username: string) =>
    request<{ message: string }>(`/playlists/${playlistId}/invite`, { method: "POST", body: JSON.stringify({ username }) }),
  acceptPlaylistInvite: (playlistId: number | string) =>
    request<{ message: string }>(`/playlists/${playlistId}/accept-invite`, { method: "POST" }),
  declinePlaylistInvite: (playlistId: number | string) =>
    request<{ message: string }>(`/playlists/${playlistId}/decline-invite`, { method: "POST" }),

  events: (location?: string) => request<EventSummary[]>(`/events${location ? `?location=${encodeURIComponent(location)}` : ""}`),
  myEvents: () => request<{ events: MyEventSummary[]; count: number }>("/events/my-events"),
  eventLocations: () => request<{ locations: { value: string; label: string }[] }>("/events/locations"),
  event: (id: number | string) => request<EventDetail>(`/events/${id}`),
  eventTracks: (id: number | string) => request<EventTracksResponse>(`/events/${id}/tracks`),
  createEvent: (data: { title: string; location: string; eventStartTime: string; isPublic?: boolean }) =>
    request<{ id: number; title: string; message: string }>("/events/create", { method: "POST", body: JSON.stringify(data) }),
  joinEvent: (id: number | string) => request(`/events/${id}/join`, { method: "POST" }),
  voteTrack: (eventId: number | string, trackId: string) =>
    request(`/events/${eventId}/tracks/${encodeURIComponent(trackId)}/vote`, { method: "POST" }),
  unvoteTrack: (eventId: number | string, trackId: string) =>
    request(`/events/${eventId}/tracks/${encodeURIComponent(trackId)}/unvote`, { method: "DELETE" }),
  addTrackToEvent: (eventId: number | string, trackId: string) =>
    request(`/events/${eventId}/tracks/${encodeURIComponent(trackId)}/add`, { method: "POST" }),
  changeVoteLicense: (eventId: number | string, voteLicense: string, voteWindowStart?: string, voteWindowEnd?: string) =>
    request<{ message: string }>(`/events/${eventId}/vote-license`, {
      method: "PUT",
      body: JSON.stringify({ voteLicense, voteWindowStart, voteWindowEnd }),
    }),
  eventRoles: (eventId: number | string) => request<EventRolesResponse>(`/events/${eventId}/roles`),
  assignEditor: (eventId: number | string, userId: number) =>
    request<{ message: string }>(`/events/${eventId}/assign-editor/${userId}`, { method: "POST" }),
  transferOwnership: (eventId: number | string, userId: number) =>
    request<{ message: string }>(`/events/${eventId}/transfer-ownership/${userId}`, { method: "POST" }),
  removeUserRole: (eventId: number | string, userId: number) =>
    request<{ message: string }>(`/events/${eventId}/remove-user-role/${userId}`, { method: "DELETE" }),
  inviteToEvent: (eventId: number | string, userId: number, role: "organizer" | "manager" | "attendee") =>
    request<{ message: string }>(`/events/${eventId}/invite`, { method: "POST", body: JSON.stringify({ userId, role }) }),
  eventPendingInvites: (eventId: number | string) =>
    request<{ pendingInvites: PendingEventInvite[]; count: number }>(`/events/${eventId}/pending-invites`),
  acceptEventInvite: (eventId: number | string) =>
    request<{ message: string }>(`/events/${eventId}/accept-invite`, { method: "POST" }),
  declineEventInvite: (eventId: number | string) =>
    request<{ message: string }>(`/events/${eventId}/decline-invite`, { method: "POST" }),

  registerDevice: (deviceId: string, name: string, platform: string, appVersion: string) =>
    request<Device>("/devices/register", { method: "POST", body: JSON.stringify({ deviceId, name, platform, appVersion }) }),
  myDevices: () => request<Device[]>("/devices/my-devices"),
  delegatedToMe: () => request<Device[]>("/devices/delegated-to-me"),
  delegateDevice: (deviceId: string, email: string) =>
    request<{ message: string }>(`/devices/${deviceId}/delegate`, { method: "POST", body: JSON.stringify({ email }) }),
  revokeDevice: (deviceId: string, userId: number) =>
    request<{ message: string }>(`/devices/${deviceId}/delegate/${userId}`, { method: "DELETE" }),
  sendDeviceCommand: (deviceId: string, action: "play" | "pause" | "skip") =>
    request<{ message: string }>(`/devices/${deviceId}/control/${action}`, { method: "POST" }),
};
