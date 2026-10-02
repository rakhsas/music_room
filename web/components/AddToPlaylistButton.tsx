"use client";

import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { api, ApiError } from "@/lib/api";
import type { JamendoTrack, PlaylistSummary } from "@/lib/types";
import { PlusIcon } from "./icons";

// Quick "add to playlist" action usable anywhere a track shows up (home/search cards, playlist
// and event track rows) - not just from inside a playlist's own add-track search box.
//
// The menu is portaled to document.body with fixed positioning computed from the trigger's own
// rect, rather than being an absolutely-positioned child. Track cards live inside a CSS
// perspective/preserve-3d transform (see ui/3d-card.tsx) for the tilt effect, and a normal
// absolute-positioned dropdown nested in that transform context renders behind sibling content
// and gets visually clipped/composited wrong - portaling escapes that stacking context entirely.
export function AddToPlaylistButton({
  track,
  className,
  variant = "overlay",
}: {
  track: JamendoTrack;
  className?: string;
  /** "overlay" = circular translucent button atop artwork (cards). "inline" = plain icon button for list rows. */
  variant?: "overlay" | "inline";
}) {
  const [open, setOpen] = useState(false);
  const [menuPos, setMenuPos] = useState({ top: 0, left: 0 });
  const [openUpward, setOpenUpward] = useState(false);
  const [playlists, setPlaylists] = useState<PlaylistSummary[] | null>(null);
  const [addedTo, setAddedTo] = useState<Set<number>>(new Set());
  const [error, setError] = useState<string | null>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      const target = e.target as Node;
      if (buttonRef.current?.contains(target) || menuRef.current?.contains(target)) return;
      setOpen(false);
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  async function handleOpen(e: React.MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    if (!open && buttonRef.current) {
      const rect = buttonRef.current.getBoundingClientRect();
      const menuWidth = 224; // w-56
      const menuHeightEstimate = 260;
      const upward = rect.bottom + menuHeightEstimate > window.innerHeight;
      setOpenUpward(upward);
      setMenuPos({
        top: upward ? rect.top - 8 : rect.bottom + 8,
        left: Math.min(rect.right - menuWidth, window.innerWidth - menuWidth - 8),
      });
    }
    setOpen((v) => !v);
    if (!playlists) {
      try {
        const { playlists: all } = await api.myPlaylists();
        setPlaylists(all.filter((p) => p.canEdit));
      } catch {
        setError("Couldn't load your playlists");
      }
    }
  }

  async function handleAdd(e: React.MouseEvent, playlistId: number) {
    e.preventDefault();
    e.stopPropagation();
    try {
      await api.addTrackToPlaylist(playlistId, track.id);
      setAddedTo((prev) => new Set(prev).add(playlistId));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't add track");
    }
  }

  return (
    <div className={className}>
      <button
        ref={buttonRef}
        onClick={handleOpen}
        className={
          variant === "overlay"
            ? "flex h-9 w-9 items-center justify-center rounded-full bg-white/10 text-text-primary backdrop-blur transition-colors hover:bg-white/20"
            : "flex h-8 w-8 items-center justify-center text-text-tertiary transition-colors hover:text-text-primary"
        }
        aria-label="Add to playlist"
      >
        <PlusIcon className="h-4 w-4" />
      </button>

      {open &&
        typeof document !== "undefined" &&
        createPortal(
          <div
            ref={menuRef}
            style={{ top: menuPos.top, left: menuPos.left, transform: openUpward ? "translateY(-100%)" : undefined }}
            className="glass fixed z-50 w-56 rounded-xl border border-white/10 py-2 shadow-2xl"
          >
            <p className="px-3 pb-1.5 text-xs font-bold uppercase tracking-widest text-text-tertiary">Add to playlist</p>
            {error && <p className="px-3 py-2 text-xs text-error">{error}</p>}
            {!playlists && !error && <p className="px-3 py-2 text-xs text-text-secondary">Loading…</p>}
            {playlists?.length === 0 && <p className="px-3 py-2 text-xs text-text-secondary">No editable playlists yet.</p>}
            <div className="scrollbar-thin max-h-56 overflow-y-auto">
              {playlists?.map((p) => (
                <button
                  key={p.id}
                  onClick={(e) => handleAdd(e, p.id)}
                  disabled={addedTo.has(p.id)}
                  className="flex w-full items-center justify-between px-3 py-2 text-left text-sm text-text-primary transition-colors hover:bg-white/5 disabled:text-text-tertiary"
                >
                  <span className="truncate">{p.name}</span>
                  {addedTo.has(p.id) && <span className="shrink-0 text-xs text-primary">Added</span>}
                </button>
              ))}
            </div>
          </div>,
          document.body,
        )}
    </div>
  );
}
