"use client";

import Image from "next/image";
import { usePlayer } from "@/lib/player-context";
import type { JamendoTrack } from "@/lib/types";
import { CardBody, CardContainer, CardItem } from "./ui/3d-card";
import { AddToPlaylistButton } from "./AddToPlaylistButton";
import { PauseIcon, PlayIcon } from "./icons";

export function TrackCard({ track, queue }: { track: JamendoTrack; queue: JamendoTrack[] }) {
  const { track: current, isPlaying, play, toggle } = usePlayer();
  const isCurrent = current?.id === track.id;

  function handlePlay() {
    if (isCurrent) toggle();
    else play(track, queue);
  }

  return (
    <CardContainer containerClassName="w-48 shrink-0 py-0">
      <CardBody
        className={`group w-48 rounded-2xl border p-4 shadow-xl transition-colors ${
          isCurrent ? "border-primary/30 bg-card" : "border-white/5 bg-surface hover:bg-card"
        }`}
      >
        {/* Art is a plain relative wrapper (not overflow-hidden) so the add-to-playlist dropdown
            isn't clipped; the image itself is clipped by an inner rounded div instead. */}
        <CardItem translateZ={50} className="relative mb-4 h-40 w-40">
          <div className="absolute inset-0 overflow-hidden rounded-xl shadow-lg">
            {track.image && <Image src={track.image} alt="" fill sizes="160px" className="object-cover" unoptimized />}
            <div className="absolute inset-0 bg-gradient-to-t from-black/40 via-transparent to-transparent opacity-0 transition-opacity group-hover:opacity-100" />
          </div>
          <CardItem
            as="button"
            translateZ={80}
            onClick={handlePlay}
            className="absolute bottom-3 right-3 flex h-12 w-12 translate-y-2 items-center justify-center rounded-full bg-gradient-to-br from-primary to-accent text-background opacity-0 shadow-xl shadow-primary/40 transition-all group-hover:translate-y-0 group-hover:opacity-100"
            aria-label={isCurrent && isPlaying ? "Pause" : "Play"}
          >
            {isCurrent && isPlaying ? <PauseIcon className="h-5 w-5" /> : <PlayIcon className="h-5 w-5 translate-x-0.5" />}
          </CardItem>
          <CardItem
            translateZ={80}
            className="absolute right-2 top-2 -translate-y-2 opacity-0 transition-all group-hover:translate-y-0 group-hover:opacity-100"
          >
            <AddToPlaylistButton track={track} />
          </CardItem>
        </CardItem>
        <CardItem translateZ={25} as="p" className={`truncate text-sm font-bold ${isCurrent ? "text-primary" : "text-text-primary"}`}>
          {track.name}
        </CardItem>
        <CardItem translateZ={25} as="p" className="truncate text-xs text-text-secondary">
          {track.artist_name}
        </CardItem>
      </CardBody>
    </CardContainer>
  );
}
