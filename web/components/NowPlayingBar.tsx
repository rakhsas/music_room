"use client";

import Image from "next/image";
import { usePlayer } from "@/lib/player-context";
import { formatDuration } from "@/lib/format";
import { BackgroundGradient } from "./ui/background-gradient";
import { AddToPlaylistButton } from "./AddToPlaylistButton";
import { AddToEventButton } from "./AddToEventButton";
import { NextIcon, PauseIcon, PlayIcon, PrevIcon, VolumeIcon } from "./icons";

export function NowPlayingBar() {
  const { track, isPlaying, currentTime, duration, volume, toggle, seek, setVolume, next, previous } = usePlayer();

  return (
    <footer className="glass relative z-20 flex h-24 shrink-0 items-center gap-4 border-t border-white/5 px-5">
      <div className="flex w-72 min-w-0 items-center gap-3">
        {track ? (
          <>
            <BackgroundGradient containerClassName="shrink-0" className="relative block h-14 w-14 overflow-hidden rounded-lg bg-card">
              {track.image && <Image src={track.image} alt="" fill sizes="56px" className="object-cover" unoptimized />}
            </BackgroundGradient>
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-text-primary">{track.name}</p>
              <p className="truncate text-xs text-text-secondary">{track.artist_name}</p>
            </div>
            <AddToPlaylistButton track={track} variant="inline" />
            <AddToEventButton track={track} />
          </>
        ) : (
          <p className="text-xs text-text-tertiary">Select a song to play</p>
        )}
      </div>

      <div className="flex flex-1 flex-col items-center gap-2">
        <div className="flex items-center gap-5">
          <button onClick={previous} disabled={!track} className="text-text-secondary transition-colors hover:text-text-primary disabled:opacity-30" aria-label="Previous">
            <PrevIcon className="h-5 w-5" />
          </button>
          <button
            onClick={toggle}
            disabled={!track}
            className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-primary to-primary-deep text-background shadow-lg shadow-primary/30 transition-transform hover:scale-105 disabled:opacity-30 disabled:hover:scale-100"
            aria-label={isPlaying ? "Pause" : "Play"}
          >
            {isPlaying ? <PauseIcon className="h-4 w-4" /> : <PlayIcon className="h-4 w-4 translate-x-0.5" />}
          </button>
          <button onClick={next} disabled={!track} className="text-text-secondary transition-colors hover:text-text-primary disabled:opacity-30" aria-label="Next">
            <NextIcon className="h-5 w-5" />
          </button>
        </div>
        <div className="flex w-full max-w-xl items-center gap-2 text-xs text-text-tertiary">
          <span className="w-10 text-right tabular-nums">{formatDuration(currentTime)}</span>
          <input
            type="range"
            min={0}
            max={duration || 0}
            value={currentTime}
            onChange={(e) => seek(Number(e.target.value))}
            disabled={!track}
            className="h-1 flex-1 accent-primary"
          />
          <span className="w-10 tabular-nums">{formatDuration(duration)}</span>
        </div>
      </div>

      <div className="flex w-40 items-center justify-end gap-2">
        <VolumeIcon className="h-4 w-4 text-text-secondary" />
        <input
          type="range"
          min={0}
          max={1}
          step={0.01}
          value={volume}
          onChange={(e) => setVolume(Number(e.target.value))}
          className="h-1 w-24 accent-primary"
        />
      </div>
    </footer>
  );
}
