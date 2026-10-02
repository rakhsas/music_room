"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";
import { NotificationBell } from "./NotificationBell";
import { LogOutIcon, UserIcon } from "./icons";

export function Topbar() {
  const { user, logout } = useAuth();
  const router = useRouter();

  function handleLogout() {
    logout();
    router.push("/login");
  }

  return (
    <header className="glass sticky top-0 z-20 flex h-16 shrink-0 items-center justify-end gap-4 border-b border-white/5 px-6">
      {user?.isPremium && (
        <span className="rounded-full bg-gradient-to-r from-primary to-purple px-3 py-1 text-xs font-bold uppercase tracking-wide text-background shadow-lg shadow-primary/20">
          Premium
        </span>
      )}
      <NotificationBell />
      <Link
        href="/profile"
        className="flex items-center gap-2 rounded-full bg-white/5 py-1 pl-1 pr-3 text-sm font-medium text-text-primary transition-colors hover:bg-white/10"
      >
        <span className="flex h-7 w-7 items-center justify-center rounded-full bg-gradient-to-br from-primary-deep to-purple">
          <UserIcon className="h-4 w-4 text-white" />
        </span>
        {user?.fullName ?? "Profile"}
      </Link>
      <button
        onClick={handleLogout}
        className="flex h-9 w-9 items-center justify-center rounded-full text-text-secondary transition-colors hover:bg-white/5 hover:text-text-primary"
        aria-label="Log out"
      >
        <LogOutIcon className="h-5 w-5" />
      </button>
    </header>
  );
}
