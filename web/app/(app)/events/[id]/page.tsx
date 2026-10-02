"use client";

import { use, useCallback, useEffect, useState } from "react";
import { api, ApiError } from "@/lib/api";
import type { EventDetail, JamendoTrack } from "@/lib/types";
import { TrackRow } from "@/components/TrackRow";
import { EventManagePanel } from "@/components/EventManagePanel";
import { SearchIcon, ThumbUpIcon } from "@/components/icons";

export default function EventPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);

  const [detail, setDetail] = useState<EventDetail | null>(null);
  const [tracks, setTracks] = useState<JamendoTrack[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busyTrackId, setBusyTrackId] = useState<string | null>(null);
  const [joining, setJoining] = useState(false);

  const [query, setQuery] = useState("");
  const [results, setResults] = useState<JamendoTrack[]>([]);

  const load = useCallback(async () => {
    try {
      const [d, t] = await Promise.all([api.event(id), api.eventTracks(id)]);
      setDetail(d);
      setTracks(t.tracks);
    } catch {
      setError("Couldn't load this event.");
    }
  }, [id]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- initial fetch-on-mount, not a render loop
    load();
  }, [load]);

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

  const voteState = new Map(detail?.songs.map((s) => [s.trackId, s]) ?? []);

  async function toggleVote(trackId: string) {
    setBusyTrackId(trackId);
    try {
      if (voteState.get(trackId)?.hasUserVoted) {
        await api.unvoteTrack(id, trackId);
      } else {
        await api.voteTrack(id, trackId);
      }
      await load();
    } catch (err) {
      window.alert(err instanceof ApiError ? err.message : "Couldn't update your vote");
    } finally {
      setBusyTrackId(null);
    }
  }

  async function addTrack(track: JamendoTrack) {
    setBusyTrackId(track.id);
    try {
      await api.addTrackToEvent(id, track.id);
      await load();
      setQuery("");
      setResults([]);
    } catch (err) {
      window.alert(err instanceof ApiError ? err.message : "Couldn't add track");
    } finally {
      setBusyTrackId(null);
    }
  }

  async function handleJoin() {
    setJoining(true);
    try {
      await api.joinEvent(id);
      await load();
    } catch (err) {
      window.alert(err instanceof ApiError ? err.message : "Couldn't join event");
    } finally {
      setJoining(false);
    }
  }

  if (error) return <p className="pt-8 text-text-secondary">{error}</p>;
  if (!detail) return <p className="pt-8 text-text-secondary">Loading…</p>;

  const sortedTracks = [...tracks].sort((a, b) => (voteState.get(b.id)?.voteCount ?? 0) - (voteState.get(a.id)?.voteCount ?? 0));
  const canAddTracks = detail.currentUserRole !== null;
  const canManage = detail.currentUserRole === "owner" || detail.currentUserRole === "editor";

  return (
    <div className="pt-6">
      <div className="mb-8 flex items-end justify-between gap-6 rounded-3xl border border-white/5 bg-gradient-to-br from-surface via-card to-surface p-8 shadow-2xl">
        <div>
          <p className="inline-block rounded-full bg-primary/15 px-2.5 py-1 text-xs font-bold uppercase tracking-widest text-primary">
            {detail.location}
          </p>
          <h1 className="mb-2 mt-3 text-4xl font-extrabold tracking-tight text-text-primary md:text-5xl">{detail.title}</h1>
          <p className="text-sm text-text-secondary">
            {detail.organizer.name} · {detail.attendeeCount} attending · {new Date(detail.eventStartTime).toLocaleString()}
          </p>
          {detail.description && <p className="mt-2 max-w-xl text-sm text-text-secondary">{detail.description}</p>}
        </div>
        {!detail.currentUserRole && (
          <button
            onClick={handleJoin}
            disabled={joining}
            className="shrink-0 rounded-full bg-gradient-to-r from-primary to-primary-deep px-4 py-2 text-sm font-semibold text-background shadow-lg shadow-primary/25 transition-transform hover:scale-[1.02] disabled:opacity-50"
          >
            {joining ? "Joining…" : "Join event"}
          </button>
        )}
      </div>

      {canManage && <EventManagePanel eventId={id} currentUserRole={detail.currentUserRole as "owner" | "editor"} onChanged={load} />}

      {canAddTracks && (
        <div className="mb-6 max-w-md">
          <div className="glass mb-2 flex items-center gap-3 rounded-full border border-white/10 px-4 py-2.5">
            <SearchIcon className="h-4 w-4 text-text-secondary" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Add a song for people to vote on"
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

      <h2 className="mb-3 text-lg font-bold text-text-primary">Tracks — vote for what plays next</h2>
      <div className="flex flex-col">
        {sortedTracks.map((t, i) => {
          const state = voteState.get(t.id);
          return (
            <TrackRow
              key={t.id}
              track={t}
              index={i}
              queue={sortedTracks}
              trailing={
                <button
                  onClick={() => toggleVote(t.id)}
                  disabled={busyTrackId === t.id}
                  aria-label={state?.hasUserVoted ? "Remove your vote" : "Vote for this track"}
                  className={`flex items-center gap-1 rounded-full px-2 py-1 text-xs font-semibold transition-colors disabled:opacity-50 ${
                    state?.hasUserVoted ? "bg-primary/20 text-primary" : "text-text-secondary hover:text-text-primary"
                  }`}
                >
                  <ThumbUpIcon className="h-3.5 w-3.5" />
                  {state?.voteCount ?? 0}
                </button>
              }
            />
          );
        })}
        {sortedTracks.length === 0 && <p className="py-8 text-center text-text-secondary">No tracks added yet.</p>}
      </div>
    </div>
  );
}
