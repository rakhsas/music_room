import Link from "next/link";
import type { EventSummary } from "@/lib/types";

export function EventCard({ event }: { event: EventSummary }) {
  const date = new Date(event.eventStartTime);
  return (
    <Link
      href={`/events/${event.id}`}
      className="group relative w-56 shrink-0 overflow-hidden rounded-2xl border border-white/5 bg-surface p-5 shadow-xl transition-all hover:-translate-y-1 hover:border-primary/20 hover:bg-card"
    >
      <div className="absolute -right-8 -top-8 h-24 w-24 rounded-full bg-gradient-to-br from-primary/20 to-purple/20 blur-2xl transition-opacity group-hover:opacity-100" />
      <p className="relative mb-2 inline-block rounded-full bg-primary/15 px-2.5 py-1 text-[10px] font-bold uppercase tracking-widest text-primary">
        {event.location}
      </p>
      <p className="relative truncate text-base font-bold text-text-primary">{event.title}</p>
      <p className="relative mt-1 text-xs text-text-secondary">
        {date.toLocaleDateString(undefined, { month: "short", day: "numeric" })} · {event.attendeeCount} attending
      </p>
      <p className="relative truncate text-xs text-text-tertiary">by {event.organizer}</p>
    </Link>
  );
}
