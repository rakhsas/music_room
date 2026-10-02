"use client";

import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { api, ApiError } from "@/lib/api";
import type { EventNotification, PlaylistNotification } from "@/lib/types";
import { BellIcon } from "./icons";

// Same portal-to-body pattern as AddToPlaylistButton - avoids clipping/stacking issues from any
// ancestor's overflow/transform.
export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const [pos, setPos] = useState({ top: 0, left: 0 });
  const [playlistInvites, setPlaylistInvites] = useState<PlaylistNotification[]>([]);
  const [eventInvites, setEventInvites] = useState<EventNotification[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [busyKey, setBusyKey] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);

  async function load() {
    try {
      const home = await api.home();
      setPlaylistInvites(home.notifications.playlistNotifications ?? []);
      setEventInvites(home.notifications.eventNotifications ?? []);
    } catch {
      // notifications are a convenience, not critical - fail quietly
    } finally {
      setLoaded(true);
    }
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- initial fetch-on-mount, not a render loop
    load();
  }, []);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      const target = e.target as Node;
      if (buttonRef.current?.contains(target) || menuRef.current?.contains(target)) return;
      setOpen(false);
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  function toggleOpen() {
    if (!open && buttonRef.current) {
      const rect = buttonRef.current.getBoundingClientRect();
      const menuWidth = 320;
      setPos({ top: rect.bottom + 8, left: Math.min(rect.right - menuWidth, window.innerWidth - menuWidth - 8) });
    }
    setOpen((v) => !v);
  }

  async function respondToPlaylist(n: PlaylistNotification, accept: boolean) {
    const key = `p-${n.playlistId}`;
    setBusyKey(key);
    setError(null);
    try {
      if (accept) await api.acceptPlaylistInvite(n.playlistId);
      else await api.declinePlaylistInvite(n.playlistId);
      setPlaylistInvites((prev) => prev.filter((p) => p.playlistId !== n.playlistId));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't respond to invite");
    } finally {
      setBusyKey(null);
    }
  }

  async function respondToEvent(n: EventNotification, accept: boolean) {
    const key = `e-${n.eventId}`;
    setBusyKey(key);
    setError(null);
    try {
      if (accept) await api.acceptEventInvite(n.eventId);
      else await api.declineEventInvite(n.eventId);
      setEventInvites((prev) => prev.filter((e) => e.eventId !== n.eventId));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't respond to invite");
    } finally {
      setBusyKey(null);
    }
  }

  const count = playlistInvites.length + eventInvites.length;

  return (
    <>
      <button
        ref={buttonRef}
        onClick={toggleOpen}
        className="relative flex h-9 w-9 items-center justify-center rounded-full text-text-secondary transition-colors hover:bg-white/5 hover:text-text-primary"
        aria-label="Notifications"
      >
        <BellIcon className="h-5 w-5" />
        {loaded && count > 0 && (
          <span className="absolute right-1 top-1 flex h-4 w-4 items-center justify-center rounded-full bg-coral text-[10px] font-bold text-white">
            {count}
          </span>
        )}
      </button>

      {open &&
        typeof document !== "undefined" &&
        createPortal(
          <div
            ref={menuRef}
            style={{ top: pos.top, left: pos.left }}
            className="glass fixed z-50 w-80 rounded-xl border border-white/10 py-2 shadow-2xl"
          >
            <p className="px-3 pb-1.5 text-xs font-bold uppercase tracking-widest text-text-tertiary">Notifications</p>
            {error && <p className="px-3 py-2 text-xs text-error">{error}</p>}
            {loaded && count === 0 && <p className="px-3 py-2 text-xs text-text-secondary">You&apos;re all caught up.</p>}

            <div className="scrollbar-thin max-h-80 overflow-y-auto">
              {playlistInvites.map((n) => (
                <div key={`p-${n.playlistId}`} className="border-t border-white/5 px-3 py-2.5">
                  <p className="text-sm text-text-primary">{n.message}</p>
                  <p className="text-xs text-text-tertiary">{n.note}</p>
                  <div className="mt-2 flex gap-2">
                    <button
                      onClick={() => respondToPlaylist(n, true)}
                      disabled={busyKey === `p-${n.playlistId}`}
                      className="rounded-full bg-primary px-3 py-1 text-xs font-semibold text-background disabled:opacity-50"
                    >
                      Accept
                    </button>
                    <button
                      onClick={() => respondToPlaylist(n, false)}
                      disabled={busyKey === `p-${n.playlistId}`}
                      className="rounded-full border border-white/10 px-3 py-1 text-xs font-semibold text-text-secondary disabled:opacity-50"
                    >
                      Decline
                    </button>
                  </div>
                </div>
              ))}
              {eventInvites.map((n) => (
                <div key={`e-${n.eventId}`} className="border-t border-white/5 px-3 py-2.5">
                  <p className="text-sm text-text-primary">
                    {n.inviterName} invited you to <span className="font-semibold">{n.eventTitle}</span> as {n.invitedRole}
                  </p>
                  <div className="mt-2 flex gap-2">
                    <button
                      onClick={() => respondToEvent(n, true)}
                      disabled={busyKey === `e-${n.eventId}`}
                      className="rounded-full bg-primary px-3 py-1 text-xs font-semibold text-background disabled:opacity-50"
                    >
                      Accept
                    </button>
                    <button
                      onClick={() => respondToEvent(n, false)}
                      disabled={busyKey === `e-${n.eventId}`}
                      className="rounded-full border border-white/10 px-3 py-1 text-xs font-semibold text-text-secondary disabled:opacity-50"
                    >
                      Decline
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>,
          document.body,
        )}
    </>
  );
}
