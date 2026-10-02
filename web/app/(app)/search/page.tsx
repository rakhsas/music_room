"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { JamendoArtist, JamendoTrack } from "@/lib/types";
import { Section } from "@/components/Section";
import { TrackCard } from "@/components/TrackCard";
import { ArtistCard } from "@/components/ArtistCard";
import { SearchIcon } from "@/components/icons";

export default function SearchPage() {
  const [query, setQuery] = useState("");
  const [tracks, setTracks] = useState<JamendoTrack[]>([]);
  const [artists, setArtists] = useState<JamendoArtist[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const trimmed = query.trim();
    if (!trimmed) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- clearing stale results when the query empties, not a render loop
      setTracks([]);
      setArtists([]);
      return;
    }
    setLoading(true);
    const timeout = setTimeout(() => {
      Promise.all([api.searchTracks(trimmed), api.searchArtists(trimmed)])
        .then(([trackRes, artistRes]) => {
          setTracks(trackRes.results ?? []);
          setArtists(artistRes.results ?? []);
        })
        .catch(() => {
          setTracks([]);
          setArtists([]);
        })
        .finally(() => setLoading(false));
    }, 350);
    return () => clearTimeout(timeout);
  }, [query]);

  return (
    <div className="pt-6">
      <div className="glass mb-8 flex max-w-md items-center gap-3 rounded-full border border-white/10 px-5 py-3.5 shadow-xl focus-within:border-primary/30">
        <SearchIcon className="h-5 w-5 text-text-secondary" />
        <input
          autoFocus
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="What do you want to listen to?"
          className="w-full bg-transparent text-sm text-text-primary placeholder:text-text-tertiary focus:outline-none"
        />
      </div>

      {loading && <p className="text-text-secondary">Searching…</p>}

      {!loading && query.trim() && tracks.length === 0 && artists.length === 0 && (
        <p className="text-text-secondary">No results for &ldquo;{query}&rdquo;.</p>
      )}

      {tracks.length > 0 && (
        <Section title="Songs">
          {tracks.map((t) => (
            <TrackCard key={t.id} track={t} queue={tracks} />
          ))}
        </Section>
      )}

      {artists.length > 0 && (
        <Section title="Artists">
          {artists.map((a) => (
            <ArtistCard key={a.id} artist={a} />
          ))}
        </Section>
      )}
    </div>
  );
}
