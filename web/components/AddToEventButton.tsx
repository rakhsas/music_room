"use client";

import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { api, ApiError } from "@/lib/api";
import type { JamendoTrack, MyEventSummary } from "@/lib/types";
import { PlusIcon } from "./icons";

// Sibling of AddToPlaylistButton.tsx - same portal-to-body pattern (see that file's comment for
// why), targeting POST /events/:id/tracks/:trackId/add instead. Lists events the user can add
// tracks to (organizer/manager/attendee - GET /events/my-events already scopes to that).
export function AddToEventButton({ track, className }: { track: JamendoTrack; className?: string }) {
  const [open, setOpen] = useState(false);
  const [pos, setPos] = useState({ top: 0, left: 0 });
  const [events, setEvents] = useState<MyEventSummary[] | null>(null);
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
      const menuWidth = 224;
      setPos({ top: rect.top - 8, left: Math.min(rect.right - menuWidth, window.innerWidth - menuWidth - 8) });
    }
    setOpen((v) => !v);
    if (!events) {
      try {
        const res = await api.myEvents();
        setEvents(res.events);
      } catch {
        setError("Couldn't load your events");
      }
    }
  }

  async function handleAdd(e: React.MouseEvent, eventId: number) {
    e.preventDefault();
    e.stopPropagation();
    try {
      await api.addTrackToEvent(eventId, track.id);
      setAddedTo((prev) => new Set(prev).add(eventId));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't add track");
    }
  }

  return (
    <div className={className}>
      <button
        ref={buttonRef}
        onClick={handleOpen}
        className="flex h-9 w-9 items-center justify-center rounded-full text-text-secondary transition-colors hover:bg-white/5 hover:text-text-primary"
        aria-label="Add to event"
      >
        <PlusIcon className="h-4 w-4" />
      </button>

      {open &&
        typeof document !== "undefined" &&
        createPortal(
          <div
            ref={menuRef}
            style={{ top: pos.top, left: pos.left, transform: "translateY(-100%)" }}
            className="glass fixed z-50 w-56 rounded-xl border border-white/10 py-2 shadow-2xl"
          >
            <p className="px-3 pb-1.5 text-xs font-bold uppercase tracking-widest text-text-tertiary">Add to event</p>
            {error && <p className="px-3 py-2 text-xs text-error">{error}</p>}
            {!events && !error && <p className="px-3 py-2 text-xs text-text-secondary">Loading…</p>}
            {events?.length === 0 && <p className="px-3 py-2 text-xs text-text-secondary">Join or create an event first.</p>}
            <div className="scrollbar-thin max-h-56 overflow-y-auto">
              {events?.map((e) => (
                <button
                  key={e.id}
                  onClick={(ev) => handleAdd(ev, e.id)}
                  disabled={addedTo.has(e.id)}
                  className="flex w-full items-center justify-between px-3 py-2 text-left text-sm text-text-primary transition-colors hover:bg-white/5 disabled:text-text-tertiary"
                >
                  <span className="truncate">{e.title}</span>
                  {addedTo.has(e.id) && <span className="shrink-0 text-xs text-primary">Added</span>}
                </button>
              ))}
            </div>
          </div>,
          document.body,
        )}
    </div>
  );
}
