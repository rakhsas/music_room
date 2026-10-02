"use client";

import { createContext, useCallback, useContext, useEffect, useRef, useState } from "react";
import type { JamendoTrack } from "./types";

interface PlayerContextValue {
  track: JamendoTrack | null;
  queue: JamendoTrack[];
  isPlaying: boolean;
  currentTime: number;
  duration: number;
  volume: number;
  play: (track: JamendoTrack, queue?: JamendoTrack[]) => void;
  toggle: () => void;
  seek: (time: number) => void;
  setVolume: (v: number) => void;
  next: () => void;
  previous: () => void;
}

const PlayerContext = createContext<PlayerContextValue | null>(null);

export function PlayerProvider({ children }: { children: React.ReactNode }) {
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const [track, setTrack] = useState<JamendoTrack | null>(null);
  const [queue, setQueue] = useState<JamendoTrack[]>([]);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolumeState] = useState(0.8);

  useEffect(() => {
    const audio = new Audio();
    audio.volume = volume;
    audioRef.current = audio;

    const onTimeUpdate = () => setCurrentTime(audio.currentTime);
    const onLoadedMetadata = () => setDuration(audio.duration || 0);
    const onEnded = () => setIsPlaying(false);

    audio.addEventListener("timeupdate", onTimeUpdate);
    audio.addEventListener("loadedmetadata", onLoadedMetadata);
    audio.addEventListener("ended", onEnded);

    return () => {
      audio.removeEventListener("timeupdate", onTimeUpdate);
      audio.removeEventListener("loadedmetadata", onLoadedMetadata);
      audio.removeEventListener("ended", onEnded);
      audio.pause();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const play = useCallback((newTrack: JamendoTrack, newQueue?: JamendoTrack[]) => {
    const audio = audioRef.current;
    if (!audio) return;
    if (newQueue) setQueue(newQueue);
    setTrack(newTrack);
    audio.src = newTrack.audio;
    audio.play().catch(() => {});
    setIsPlaying(true);
  }, []);

  const toggle = useCallback(() => {
    const audio = audioRef.current;
    if (!audio || !track) return;
    if (isPlaying) {
      audio.pause();
      setIsPlaying(false);
    } else {
      audio.play().catch(() => {});
      setIsPlaying(true);
    }
  }, [isPlaying, track]);

  const seek = useCallback((time: number) => {
    const audio = audioRef.current;
    if (!audio) return;
    audio.currentTime = time;
    setCurrentTime(time);
  }, []);

  const setVolume = useCallback((v: number) => {
    const audio = audioRef.current;
    if (audio) audio.volume = v;
    setVolumeState(v);
  }, []);

  const stepQueue = useCallback(
    (direction: 1 | -1) => {
      if (!track || queue.length === 0) return;
      const idx = queue.findIndex((t) => t.id === track.id);
      if (idx === -1) return;
      const nextIdx = (idx + direction + queue.length) % queue.length;
      play(queue[nextIdx], queue);
    },
    [track, queue, play],
  );

  const next = useCallback(() => stepQueue(1), [stepQueue]);
  const previous = useCallback(() => stepQueue(-1), [stepQueue]);

  return (
    <PlayerContext.Provider
      value={{ track, queue, isPlaying, currentTime, duration, volume, play, toggle, seek, setVolume, next, previous }}
    >
      {children}
    </PlayerContext.Provider>
  );
}

export function usePlayer() {
  const ctx = useContext(PlayerContext);
  if (!ctx) throw new Error("usePlayer must be used within PlayerProvider");
  return ctx;
}
