"use client";

import Image from "next/image";
import { usePlayer } from "@/lib/player-context";
import { formatDuration } from "@/lib/format";
import type { JamendoTrack } from "@/lib/types";
import { AddToPlaylistButton } from "./AddToPlaylistButton";
import { PauseIcon, PlayIcon } from "./icons";

export function TrackRow({
  track,
  index,
  queue,
  trailing,
}: {
  track: JamendoTrack;
  index: number;
  queue: JamendoTrack[];
  trailing?: React.ReactNode;
}) {
  const { track: current, isPlaying, play, toggle } = usePlayer();
  const isCurrent = current?.id === track.id;

  function handlePlay() {
    if (isCurrent) {
      toggle();
    } else {
      play(track, queue);
    }
  }

  return (
    <div
      className={`group grid grid-cols-[2rem_1fr_auto] items-center gap-4 rounded-xl px-3 py-2.5 transition-colors hover:bg-white/[0.04] ${
        isCurrent ? "bg-white/[0.04]" : ""
      }`}
    >
      <button
        onClick={handlePlay}
        className="flex h-8 w-8 items-center justify-center text-text-secondary hover:text-text-primary"
        aria-label={isCurrent && isPlaying ? "Pause" : "Play"}
      >
        <span className={isCurrent ? "text-primary" : "group-hover:hidden"}>
          {isCurrent && isPlaying ? <PauseIcon className="h-4 w-4" /> : index + 1}
        </span>
        {!isCurrent && <PlayIcon className="hidden h-4 w-4 group-hover:block" />}
      </button>

      <div className="flex min-w-0 items-center gap-3">
        <div className="relative h-11 w-11 shrink-0 overflow-hidden rounded-lg bg-card shadow-md">
          {track.image && (
            <Image src={track.image} alt="" fill sizes="44px" className="object-cover" unoptimized />
          )}
        </div>
        <div className="min-w-0">
          <p className={`truncate text-sm font-semibold ${isCurrent ? "text-primary" : "text-text-primary"}`}>{track.name}</p>
          <p className="truncate text-xs text-text-secondary">{track.artist_name}</p>
        </div>
      </div>

      <div className="flex items-center gap-2 text-xs text-text-secondary">
        <span className="opacity-0 transition-opacity group-hover:opacity-100">
          <AddToPlaylistButton track={track} variant="inline" />
        </span>
        {trailing}
        <span className="w-10 text-right tabular-nums">{formatDuration(track.duration)}</span>
      </div>
    </div>
  );
}
