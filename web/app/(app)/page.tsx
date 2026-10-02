"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { HomeResponse } from "@/lib/types";
import { Section } from "@/components/Section";
import { TrackCard } from "@/components/TrackCard";
import { ArtistCard } from "@/components/ArtistCard";
import { PlaylistCard } from "@/components/PlaylistCard";
import { EventCard } from "@/components/EventCard";
import { Spotlight } from "@/components/ui/spotlight";
import { usePlayer } from "@/lib/player-context";
import { useAuth } from "@/lib/auth-context";
import { PlayIcon } from "@/components/icons";

function greetingForNow() {
  const hour = new Date().getHours();
  if (hour < 12) return "Good morning";
  if (hour < 18) return "Good afternoon";
  return "Good evening";
}

export default function HomePage() {
  const { user } = useAuth();
  const { play } = usePlayer();
  const [data, setData] = useState<HomeResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .home()
      .then(setData)
      .catch(() => setError("Couldn't load your home feed. Try refreshing."));
  }, []);

  if (error) return <p className="pt-8 text-text-secondary">{error}</p>;
  if (!data) return <p className="pt-8 text-text-secondary">Loading…</p>;

  const featured = data.popularSongs[0] ?? data.recommendedSongs[0];

  return (
    <div className="pt-6">
      <div className="relative mb-10 overflow-hidden rounded-3xl border border-white/5 bg-gradient-to-br from-surface via-card to-surface px-8 py-10 shadow-2xl">
        <Spotlight className="-top-20 left-1/4" fill="var(--color-primary)" />
        <p className="relative text-sm font-semibold uppercase tracking-widest text-primary">
          {greetingForNow()}
        </p>
        <h1 className="relative mt-2 text-4xl font-extrabold tracking-tight text-text-primary md:text-5xl">
          {user ? user.fullName.split(" ")[0] : "Welcome back"}
        </h1>
        <p className="relative mt-3 max-w-md text-sm text-text-secondary">
          Vote on what plays next, build playlists with friends, and pick up right where you left off.
        </p>
        {featured && (
          <button
            onClick={() => play(featured, data.popularSongs.length ? data.popularSongs : data.recommendedSongs)}
            className="relative mt-6 flex items-center gap-2 rounded-full bg-gradient-to-r from-primary to-accent px-6 py-3 text-sm font-bold text-background shadow-lg shadow-primary/30 transition-transform hover:scale-105"
          >
            <PlayIcon className="h-4 w-4" />
            Play {featured.name.trim()}
          </button>
        )}
      </div>

      {data.userPlaylists.results.length > 0 && (
        <Section title="Your Playlists">
          {data.userPlaylists.results.map((p) => (
            <PlaylistCard key={p.id} playlist={p} />
          ))}
        </Section>
      )}

      {data.recommendedSongs.length > 0 && (
        <Section title="Recommended for you">
          {data.recommendedSongs.map((t) => (
            <TrackCard key={t.id} track={t} queue={data.recommendedSongs} />
          ))}
        </Section>
      )}

      {data.popularSongs.length > 0 && (
        <Section title="Popular songs">
          {data.popularSongs.map((t) => (
            <TrackCard key={t.id} track={t} queue={data.popularSongs} />
          ))}
        </Section>
      )}

      {data.popularArtists.length > 0 && (
        <Section title="Popular artists">
          {data.popularArtists.map((a) => (
            <ArtistCard key={a.id} artist={a} />
          ))}
        </Section>
      )}

      {data.events.length > 0 && (
        <Section title="Upcoming events">
          {data.events.map((e) => (
            <EventCard key={e.id} event={e} />
          ))}
        </Section>
      )}
    </div>
  );
}
