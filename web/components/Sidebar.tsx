"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { motion } from "motion/react";
import { api, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import type { PlaylistSummary } from "@/lib/types";
import { DeviceIcon, HomeIcon, LibraryIcon, PlusIcon, SearchIcon } from "./icons";

const navItems = [
  { href: "/", label: "Home", icon: HomeIcon },
  { href: "/search", label: "Search", icon: SearchIcon },
  { href: "/playlists", label: "Playlists", icon: LibraryIcon },
  { href: "/events", label: "Events", icon: LibraryIcon },
  { href: "/devices", label: "Devices", icon: DeviceIcon },
];

export function Sidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const [playlists, setPlaylists] = useState<PlaylistSummary[]>([]);
  const [creating, setCreating] = useState(false);
  const { user } = useAuth();

  async function loadPlaylists() {
    try {
      const { playlists } = await api.myPlaylists();
      setPlaylists(playlists);
    } catch {
      // sidebar playlist shortcuts are a convenience, not critical - fail quietly
    }
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- initial fetch-on-mount, not a render loop
    loadPlaylists();
  }, []);

  async function handleCreate() {
    const name = window.prompt("Playlist name");
    if (!name) return;
    // Private playlists are Premium-only - free users just get a public one.
    const isPublic = !user?.isPremium || window.confirm("Make it public? OK = public (anyone can follow), Cancel = private (invite-only)");
    setCreating(true);
    try {
      const { id } = await api.createPlaylist(name, isPublic);
      await loadPlaylists();
      router.push(`/playlists/${id}`);
    } catch (err) {
      window.alert(err instanceof ApiError ? err.message : "Could not create playlist");
    } finally {
      setCreating(false);
    }
  }

  return (
    <aside className="glass flex h-full w-64 shrink-0 flex-col gap-2 border-r border-white/5 p-4">
      <Link href="/" className="mb-6 flex items-center gap-2.5 px-2 text-lg font-extrabold tracking-tight text-text-primary">
        <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-primary via-primary-deep to-purple text-base font-black text-background shadow-lg shadow-primary/30">
          M
        </span>
        Music Room
      </Link>

      <nav className="flex flex-col gap-1">
        {navItems.map(({ href, label, icon: Icon }) => {
          const active = href === "/" ? pathname === "/" : pathname.startsWith(href);
          return (
            <Link
              key={href}
              href={href}
              className={`relative flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition-colors ${
                active ? "text-text-primary" : "text-text-secondary hover:text-text-primary"
              }`}
            >
              {active && (
                <motion.span
                  layoutId="sidebar-active-pill"
                  className="absolute inset-0 rounded-xl bg-gradient-to-r from-white/10 to-white/[0.03]"
                  transition={{ type: "spring", bounce: 0.2, duration: 0.5 }}
                />
              )}
              <Icon className={`relative h-5 w-5 ${active ? "text-primary" : ""}`} />
              <span className="relative">{label}</span>
            </Link>
          );
        })}
      </nav>

      <div className="mt-6 flex items-center justify-between px-2">
        <p className="text-xs font-bold uppercase tracking-widest text-text-tertiary">Your Library</p>
        <button
          onClick={handleCreate}
          disabled={creating}
          className="flex h-7 w-7 items-center justify-center rounded-full bg-white/5 text-text-secondary transition-colors hover:bg-white/10 hover:text-text-primary disabled:opacity-50"
          aria-label="Create playlist"
        >
          <PlusIcon className="h-4 w-4" />
        </button>
      </div>

      <div className="scrollbar-thin flex-1 space-y-0.5 overflow-y-auto">
        {playlists.map((p) => (
          <Link
            key={p.id}
            href={`/playlists/${p.id}`}
            className={`block truncate rounded-xl px-3 py-2.5 text-sm font-medium transition-colors ${
              pathname === `/playlists/${p.id}` ? "bg-white/[0.06] text-text-primary" : "text-text-secondary hover:bg-white/[0.03] hover:text-text-primary"
            }`}
          >
            {p.name}
          </Link>
        ))}
        {playlists.length === 0 && <p className="px-3 py-2 text-xs text-text-tertiary">No playlists yet</p>}
      </div>
    </aside>
  );
}
