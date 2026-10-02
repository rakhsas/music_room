"use client";

import { use, useEffect, useState } from "react";
import Image from "next/image";
import { api } from "@/lib/api";
import type { JamendoArtist, JamendoTrack } from "@/lib/types";
import { usePlayer } from "@/lib/player-context";
import { TrackRow } from "@/components/TrackRow";
import { PlayIcon } from "@/components/icons";

export default function ArtistPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const { play } = usePlayer();

  const [artist, setArtist] = useState<JamendoArtist | null>(null);
  const [tracks, setTracks] = useState<JamendoTrack[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    api
      .artistDetail(id)
      .then((res) => {
        setArtist(res.artist);
        setTracks(res.tracks);
      })
      .catch(() => setError("Couldn't load this artist."))
      .finally(() => setLoaded(true));
  }, [id]);

  if (error) return <p className="pt-8 text-text-secondary">{error}</p>;
  if (!loaded) return <p className="pt-8 text-text-secondary">Loading…</p>;
  if (!artist) return <p className="pt-8 text-text-secondary">Artist not found.</p>;

  return (
    <div className="pt-6">
      <div className="mb-8 flex items-end gap-6 rounded-3xl border border-white/5 bg-gradient-to-br from-surface via-card to-surface p-8 shadow-2xl">
        <div className="relative h-40 w-40 shrink-0 overflow-hidden rounded-full shadow-xl ring-2 ring-white/10">
          {artist.image && <Image src={artist.image} alt="" fill sizes="160px" className="object-cover" unoptimized />}
        </div>
        <div>
          <p className="text-xs font-bold uppercase tracking-widest text-primary">Artist</p>
          <h1 className="mb-2 text-4xl font-extrabold tracking-tight text-text-primary md:text-5xl">{artist.name}</h1>
          {tracks.length > 0 && (
            <button
              onClick={() => play(tracks[0], tracks)}
              className="mt-2 flex items-center gap-2 rounded-full bg-gradient-to-r from-primary to-accent px-5 py-2.5 text-sm font-bold text-background shadow-lg shadow-primary/25 transition-transform hover:scale-[1.02]"
            >
              <PlayIcon className="h-4 w-4" />
              Play all
            </button>
          )}
        </div>
      </div>

      <div className="flex flex-col">
        {tracks.map((t, i) => (
          <TrackRow key={t.id} track={t} index={i} queue={tracks} />
        ))}
        {tracks.length === 0 && <p className="py-8 text-center text-text-secondary">No songs found for this artist.</p>}
      </div>
    </div>
  );
}
