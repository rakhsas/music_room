"use client";

import { use, useCallback, useEffect, useState } from "react";
import { io, type Socket } from "socket.io-client";
import { api, ApiError, getToken, WS_BASE_URL } from "@/lib/api";
import type { JamendoTrack, PlaylistDetail } from "@/lib/types";
import { TrackRow } from "@/components/TrackRow";
import { useAuth } from "@/lib/auth-context";
import { SearchIcon, XIcon } from "@/components/icons";

export default function PlaylistPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);

  const { user } = useAuth();
  const [detail, setDetail] = useState<PlaylistDetail | null>(null);
  const [tracks, setTracks] = useState<JamendoTrack[]>([]);
  const [error, setError] = useState<string | null>(null);

  const [query, setQuery] = useState("");
  const [results, setResults] = useState<JamendoTrack[]>([]);
  const [busyTrackId, setBusyTrackId] = useState<string | null>(null);

  const [inviteOpen, setInviteOpen] = useState(false);
  const [inviteUsername, setInviteUsername] = useState("");
  const [inviteBusy, setInviteBusy] = useState(false);
  const [inviteMessage, setInviteMessage] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [d, t] = await Promise.all([api.playlist(id), api.playlistTracks(id)]);
      setDetail(d);
      setTracks(t.tracks);
    } catch {
      setError("Couldn't load this playlist.");
    }
  }, [id]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- initial fetch-on-mount, not a render loop
    load();
  }, [load]);

  // Real-time collab: playlists.gateway.ts broadcasts track_added/track_removed/tracks_reordered
  // to everyone viewing this playlist. Reload on any of them so edits from other users show up
  // live, on top of (not instead of) the normal fetch-after-your-own-action flow above.
  useEffect(() => {
    const token = getToken();
    if (!token) return;
    const socket: Socket = io(WS_BASE_URL, { query: { playlistId: id, token }, transports: ["websocket"] });
    const onChange = () => load();
    socket.on("track_added", onChange);
    socket.on("track_removed", onChange);
    socket.on("tracks_reordered", onChange);
    return () => {
      socket.disconnect();
    };
  }, [id, load]);

  useEffect(() => {
    const trimmed = query.trim();
    if (!trimmed) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- clearing stale results when the query empties, not a render loop
      setResults([]);
      return;
    }
    const timeout = setTimeout(() => {
      api
        .searchTracks(trimmed)
        .then((res) => setResults(res.results ?? []))
        .catch(() => setResults([]));
    }, 350);
    return () => clearTimeout(timeout);
  }, [query]);

  async function addTrack(track: JamendoTrack) {
    setBusyTrackId(track.id);
    try {
      await api.addTrackToPlaylist(id, track.id);
      await load();
      setQuery("");
      setResults([]);
    } catch (err) {
      window.alert(err instanceof ApiError ? err.message : "Couldn't add track");
    } finally {
      setBusyTrackId(null);
    }
  }

  async function removeTrack(trackId: string) {
    setBusyTrackId(trackId);
    try {
      await api.removeTrackFromPlaylist(id, trackId);
      await load();
    } catch (err) {
      window.alert(err instanceof ApiError ? err.message : "Couldn't remove track");
    } finally {
      setBusyTrackId(null);
    }
  }

  async function sendInvite(e: React.FormEvent) {
    e.preventDefault();
    setInviteBusy(true);
    setInviteMessage(null);
    try {
      const res = await api.invitePlaylist(id, inviteUsername.trim());
      setInviteMessage(res.message);
      setInviteUsername("");
    } catch (err) {
      setInviteMessage(err instanceof ApiError ? err.message : "Couldn't send invite");
    } finally {
      setInviteBusy(false);
    }
  }

  async function toggleVisibility() {
    if (!detail) return;
    try {
      await api.setPlaylistVisibility(id, !detail.isPublic);
      await load();
    } catch (err) {
      window.alert(err instanceof ApiError ? err.message : "Couldn't change visibility");
    }
  }

  if (error) return <p className="pt-8 text-text-secondary">{error}</p>;
  if (!detail) return <p className="pt-8 text-text-secondary">Loading…</p>;

  const canEdit = detail.userPermissions.canEdit;

  return (
    <div className="pt-6">
      <div className="mb-8 flex items-end justify-between gap-6 rounded-3xl border border-white/5 bg-gradient-to-br from-surface via-card to-surface p-8 shadow-2xl">
        <div className="flex items-end gap-6">
          <div className="flex h-40 w-40 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-purple to-primary text-5xl shadow-xl">
            🎵
          </div>
          <div>
            <p className="text-xs font-bold uppercase tracking-widest text-primary">
              {detail.isPublic ? "Public Playlist" : "Private Playlist"}
            </p>
            <h1 className="mb-2 text-4xl font-extrabold tracking-tight text-text-primary md:text-5xl">{detail.name}</h1>
            <p className="text-sm text-text-secondary">
              {detail.owner.name} · {detail.trackCount} songs · {detail.followersCount} followers
            </p>
          </div>
        </div>
        {detail.userPermissions.isOwner && (
          <div className="flex shrink-0 gap-2">
            <button
              onClick={toggleVisibility}
              className="rounded-full border border-white/10 px-4 py-2 text-sm font-semibold text-text-primary transition-colors hover:bg-white/5"
            >
              Make {detail.isPublic ? "private" : "public"}
              {detail.isPublic && !user?.isPremium && <span className="ml-1 text-xs text-primary">Premium</span>}
            </button>
            {!detail.isPublic && (
              <button
                onClick={() => setInviteOpen((v) => !v)}
                className="rounded-full border border-white/10 px-4 py-2 text-sm font-semibold text-text-primary transition-colors hover:bg-white/5"
              >
                Invite
              </button>
            )}
          </div>
        )}
      </div>

      {inviteOpen && (
        <form onSubmit={sendInvite} className="glass mb-6 max-w-md rounded-2xl border border-white/10 p-4 shadow-xl">
          <label className="mb-1 block text-xs font-semibold text-text-secondary">Invite by username</label>
          <div className="flex gap-2">
            <input
              value={inviteUsername}
              onChange={(e) => setInviteUsername(e.target.value)}
              placeholder="username"
              className="flex-1 rounded-xl border border-white/5 bg-white/[0.03] px-3.5 py-2 text-sm text-text-primary focus:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/20"
            />
            <button
              type="submit"
              disabled={inviteBusy || !inviteUsername.trim()}
              className="rounded-full bg-gradient-to-r from-primary to-primary-deep px-4 py-2 text-sm font-semibold text-background disabled:opacity-50"
            >
              {inviteBusy ? "Sending…" : "Send"}
            </button>
          </div>
          {inviteMessage && <p className="mt-2 text-xs text-text-secondary">{inviteMessage}</p>}
        </form>
      )}

      {canEdit && (
        <div className="mb-6 max-w-md">
          <div className="glass mb-2 flex items-center gap-3 rounded-full border border-white/10 px-4 py-2.5">
            <SearchIcon className="h-4 w-4 text-text-secondary" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Add a song to this playlist"
              className="w-full bg-transparent text-sm text-text-primary placeholder:text-text-tertiary focus:outline-none"
            />
          </div>
          {results.length > 0 && (
            <div className="glass scrollbar-thin max-h-64 overflow-y-auto rounded-2xl border border-white/10 shadow-xl">
              {results.map((t) => (
                <button
                  key={t.id}
                  onClick={() => addTrack(t)}
                  disabled={busyTrackId === t.id}
                  className="flex w-full items-center justify-between px-4 py-2.5 text-left text-sm text-text-primary transition-colors hover:bg-white/5 disabled:opacity-50"
                >
                  <span className="truncate">
                    {t.name} <span className="text-text-tertiary">— {t.artist_name}</span>
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>
      )}

      <div className="flex flex-col">
        {tracks.map((t, i) => (
          <TrackRow
            key={`${t.id}-${i}`}
            track={t}
            index={i}
            queue={tracks}
            trailing={
              canEdit ? (
                <button
                  onClick={() => removeTrack(t.id)}
                  disabled={busyTrackId === t.id}
                  className="text-text-tertiary opacity-0 hover:text-error group-hover:opacity-100 disabled:opacity-50"
                  aria-label="Remove from playlist"
                >
                  <XIcon className="h-4 w-4" />
                </button>
              ) : undefined
            }
          />
        ))}
        {tracks.length === 0 && <p className="py-8 text-center text-text-secondary">No tracks yet.</p>}
      </div>
    </div>
  );
}
