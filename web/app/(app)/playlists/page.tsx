"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { PlaylistSummary } from "@/lib/types";
import { PlaylistCard } from "@/components/PlaylistCard";

export default function PublicPlaylistsPage() {
  const [playlists, setPlaylists] = useState<PlaylistSummary[] | null>(null);

  useEffect(() => {
    api.publicPlaylists().then(setPlaylists).catch(() => setPlaylists([]));
  }, []);

  return (
    <div className="pt-6">
      <h1 className="mb-6 text-2xl font-bold tracking-tight text-text-primary">Discover Playlists</h1>
      {playlists === null && <p className="text-text-secondary">Loading…</p>}
      {playlists?.length === 0 && <p className="text-text-secondary">No public playlists yet.</p>}
      <div className="flex flex-wrap gap-5">
        {playlists?.map((p) => (
          <PlaylistCard key={p.id} playlist={p} />
        ))}
      </div>
    </div>
  );
}
