"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { api, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import type { EventSummary } from "@/lib/types";
import { EventCard } from "@/components/EventCard";
import { PlusIcon } from "@/components/icons";

export default function EventsPage() {
  const router = useRouter();
  const [events, setEvents] = useState<EventSummary[] | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [locations, setLocations] = useState<{ value: string; label: string }[]>([]);
  const [title, setTitle] = useState("");
  const [location, setLocation] = useState("");
  const [startTime, setStartTime] = useState("");
  const [voteLicense, setVoteLicense] = useState<"everyone" | "invited_only" | "time_window">("everyone");
  const [voteWindowStart, setVoteWindowStart] = useState("20:00");
  const [voteWindowEnd, setVoteWindowEnd] = useState("23:00");
  const [isPrivate, setIsPrivate] = useState(false);
  const { user } = useAuth();
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    api.events().then(setEvents).catch(() => setEvents([]));
    api
      .eventLocations()
      .then((res) => {
        setLocations(res.locations);
        setLocation(res.locations[0]?.value ?? "");
      })
      .catch(() => {});
  }, []);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim() || !startTime) {
      setFormError("Title and start time are required.");
      return;
    }
    setSubmitting(true);
    setFormError(null);
    try {
      const { id } = await api.createEvent({
        title: title.trim(),
        location,
        eventStartTime: new Date(startTime).toISOString(),
        isPublic: !isPrivate,
      });
      if (voteLicense !== "everyone") {
        await api.changeVoteLicense(
          id,
          voteLicense,
          voteLicense === "time_window" ? voteWindowStart : undefined,
          voteLicense === "time_window" ? voteWindowEnd : undefined,
        );
      }
      router.push(`/events/${id}`);
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Couldn't create event");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="pt-6">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold text-text-primary">Events</h1>
        <button
          onClick={() => setShowForm((v) => !v)}
          className="flex items-center gap-2 rounded-full bg-gradient-to-r from-primary to-primary-deep px-4 py-2 text-sm font-semibold text-background shadow-lg shadow-primary/25 transition-transform hover:scale-[1.02]"
        >
          <PlusIcon className="h-4 w-4" />
          Create event
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleCreate} className="glass mb-8 max-w-md rounded-2xl border border-white/10 p-5 shadow-xl">
          <div className="mb-3">
            <label className="mb-1 block text-xs font-semibold text-text-secondary">Title</label>
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full rounded-xl border border-white/5 bg-white/[0.03] px-3.5 py-2.5 text-sm text-text-primary transition-colors focus:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/20"
            />
          </div>
          <div className="mb-3">
            <label className="mb-1 block text-xs font-semibold text-text-secondary">Location</label>
            <select
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              className="w-full rounded-xl border border-white/5 bg-white/[0.03] px-3.5 py-2.5 text-sm text-text-primary transition-colors focus:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/20"
            >
              {locations.map((l) => (
                <option key={l.value} value={l.value}>
                  {l.label}
                </option>
              ))}
            </select>
          </div>
          <div className="mb-3">
            <label className="mb-1 block text-xs font-semibold text-text-secondary">Start time</label>
            <input
              type="datetime-local"
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
              className="w-full rounded-xl border border-white/5 bg-white/[0.03] px-3.5 py-2.5 text-sm text-text-primary transition-colors focus:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/20"
            />
          </div>
          <div className="mb-3">
            <label className="mb-1 block text-xs font-semibold text-text-secondary">Who can vote</label>
            <select
              value={voteLicense}
              onChange={(e) => setVoteLicense(e.target.value as typeof voteLicense)}
              className="w-full rounded-xl border border-white/5 bg-white/[0.03] px-3.5 py-2.5 text-sm text-text-primary transition-colors focus:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/20"
            >
              <option value="everyone">Everyone</option>
              <option value="invited_only">Invited attendees only</option>
              <option value="time_window">Only during a time window</option>
            </select>
          </div>
          {voteLicense === "time_window" && (
            <div className="mb-3 grid grid-cols-2 gap-3">
              <div>
                <label className="mb-1 block text-xs font-semibold text-text-secondary">Voting opens</label>
                <input
                  type="time"
                  value={voteWindowStart}
                  onChange={(e) => setVoteWindowStart(e.target.value)}
                  className="w-full rounded-xl border border-white/5 bg-white/[0.03] px-3.5 py-2.5 text-sm text-text-primary focus:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/20"
                />
              </div>
              <div>
                <label className="mb-1 block text-xs font-semibold text-text-secondary">Voting closes</label>
                <input
                  type="time"
                  value={voteWindowEnd}
                  onChange={(e) => setVoteWindowEnd(e.target.value)}
                  className="w-full rounded-xl border border-white/5 bg-white/[0.03] px-3.5 py-2.5 text-sm text-text-primary focus:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/20"
                />
              </div>
            </div>
          )}
          <label className="mb-3 flex items-center gap-2 text-sm text-text-primary">
            <input
              type="checkbox"
              checked={isPrivate}
              disabled={!user?.isPremium}
              onChange={(e) => setIsPrivate(e.target.checked)}
              className="accent-primary"
            />
            Private event (invite-only)
            {!user?.isPremium && (
              <Link href="/profile" className="text-xs font-semibold text-primary hover:underline">
                Premium
              </Link>
            )}
          </label>
          {formError && <p className="mb-3 text-sm text-error">{formError}</p>}
          <button
            type="submit"
            disabled={submitting}
            className="rounded-full bg-gradient-to-r from-primary to-primary-deep px-4 py-2 text-sm font-semibold text-background shadow-lg shadow-primary/25 transition-transform hover:scale-[1.02] disabled:opacity-50"
          >
            {submitting ? "Creating…" : "Create"}
          </button>
        </form>
      )}

      {events === null && <p className="text-text-secondary">Loading…</p>}
      {events?.length === 0 && <p className="text-text-secondary">No public events yet.</p>}

      <div className="flex flex-wrap gap-4">
        {events?.map((e) => (
          <EventCard key={e.id} event={e} />
        ))}
      </div>
    </div>
  );
}
