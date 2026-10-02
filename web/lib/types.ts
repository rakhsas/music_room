export interface JamendoTrack {
  id: string;
  name: string;
  duration: number;
  artist_id: string;
  artist_name: string;
  album_name: string;
  album_id: string;
  album_image: string;
  image: string;
  audio: string;
  votes?: number;
}

export interface JamendoArtist {
  id: string;
  name: string;
  image: string;
  website?: string;
}

export interface JamendoListResponse<T> {
  headers: { status: string; results_count: number };
  results: T[];
}

export interface UserProfile {
  id: number;
  email: string;
  fullName: string;
  userName: string;
  avatar: string;
  bio: string;
  dateOfBirth: string | null;
  phoneNumber: string;
  profilePrivacy: string;
  emailPrivacy: string;
  phonePrivacy: string;
  googleId: string | null;
  subscriptionType: "free" | "premium";
  isPremium: boolean;
  isSubscribed: boolean;
  premiumSince: string | null;
  card: { brand: string; last4: string } | null;
  plan: { amount: number; currency: string; interval: string };
  musicPreferences: Record<string, unknown>;
  likedArtists: string[];
  likedAlbums: string[];
  likedSongs: string[];
  genres: string[];
  createdAt: string;
}

export interface Friend {
  id: number;
  fullName: string;
  userName: string;
  avatar: string;
}

export interface PaymentCard {
  number: string;
  expMonth: number;
  expYear: number;
  cvc: string;
  holderName: string;
}

export interface AuthTokens {
  access: string;
  refresh: string;
}

export interface LoginResponse {
  user: { id: number; email: string; fullName: string; userName: string; avatar: string };
  tokens: AuthTokens;
  message: string;
}

export interface PlaylistSummary {
  id: number;
  name: string;
  owner?: { id: number; name: string; avatar: string } | string;
  trackCount: number;
  followersCount: number;
  isPublic: boolean;
  canEdit?: boolean;
  createdAt: string;
}

export interface PlaylistDetail {
  id: number;
  name: string;
  owner: { id: number; name: string; avatar: string };
  tracks: string[];
  trackCount: number;
  isPublic: boolean;
  followersCount: number;
  collaborators: { id: number; name: string; avatar: string }[];
  couldEdit: string[];
  userPermissions: {
    canEdit: boolean;
    canDelete: boolean;
    isFollowing: boolean;
    isCollaborator: boolean;
    isOwner: boolean;
  };
  createdAt: string;
}

export interface PlaylistTracksResponse {
  playlistInfo: {
    id: number;
    name: string;
    owner: string;
    trackCount: number;
    isPublic: boolean;
    followersCount: number;
  };
  tracks: JamendoTrack[];
}

export interface EventSummary {
  id: number;
  title: string;
  organizer: string;
  location: string;
  attendeeCount: number;
  eventStartTime: string;
  isPublic: boolean;
}

export interface EventDetail {
  id: number;
  title: string;
  description: string;
  location: string;
  eventStartTime: string;
  eventEndTime: string | null;
  organizer: { id: number; name: string; avatar: string };
  attendeeCount: number;
  trackCount: number;
  isPublic: boolean;
  songs: { trackId: string; voteCount: number; hasUserVoted: boolean }[];
  currentUserRole: string | null;
}

export interface EventTracksResponse {
  eventInfo: {
    id: number;
    title: string;
    organizer: string;
    trackCount: number;
    attendeeCount: number;
    isPublic: boolean;
  };
  tracks: JamendoTrack[];
}

export interface PlaylistNotification {
  playlistId: number;
  playlistName: string;
  inviterId: number;
  inviterName: string;
  invitedAt: string;
  message: string;
  note: string;
}

export interface EventNotification {
  eventId: number;
  eventTitle: string;
  inviterId: number;
  inviterName: string;
  invitedRole: "organizer" | "manager" | "attendee";
  type: "event_invite";
  createdAt: string;
}

export interface HomeResponse {
  userPlaylists: { results: PlaylistSummary[] };
  recommendedSongs: JamendoTrack[];
  recentlyListened: JamendoTrack[];
  popularSongs: JamendoTrack[];
  popularArtists: JamendoArtist[];
  events: EventSummary[];
  notifications: {
    eventNotifications: EventNotification[];
    playlistNotifications: PlaylistNotification[];
  };
}

export interface OtherUser {
  id: number;
  fullName: string;
  userName: string;
  email: string;
  avatar: string;
}

export interface EventRolesResponse {
  userRoles: Record<string, "owner" | "editor" | "listener">;
  usersByRole: Record<string, { id: number; name: string; avatar: string; role: string }[]>;
}

export interface PendingEventInvite {
  userId: string;
  name: string;
  email: string;
  role: string;
  invitedAt: string;
}

export interface MyEventSummary {
  id: number;
  title: string;
  location: string;
  startTime: string;
  attendeesCount: number;
  trackCount: number;
  userRole: string;
  canEdit: boolean;
}

export interface Device {
  deviceId: string;
  name: string;
  platform?: string;
  appVersion?: string;
  lastSeenAt?: string;
  delegates?: { id: number; name: string; email?: string }[];
  owner?: { id: number; name: string };
}
