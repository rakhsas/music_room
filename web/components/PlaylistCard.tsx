import Link from "next/link";
import type { PlaylistSummary } from "@/lib/types";
import { CardBody, CardContainer, CardItem } from "./ui/3d-card";
import { LibraryIcon } from "./icons";

const GRADIENTS = [
  "from-primary to-primary-deep",
  "from-purple to-coral",
  "from-electric-blue to-purple",
  "from-accent to-primary-deep",
  "from-coral to-electric-blue",
];

export function PlaylistCard({ playlist }: { playlist: PlaylistSummary }) {
  const ownerName = typeof playlist.owner === "string" ? playlist.owner : playlist.owner?.name;
  const gradient = GRADIENTS[playlist.id % GRADIENTS.length];
  return (
    <CardContainer containerClassName="w-48 shrink-0 py-0">
      <CardItem as={Link} href={`/playlists/${playlist.id}`} translateZ={0} className="block">
        <CardBody className="w-48 rounded-2xl border border-white/5 bg-surface p-4 shadow-xl transition-colors hover:bg-card">
          <CardItem
            translateZ={50}
            className={`mb-4 flex h-40 w-40 items-center justify-center rounded-xl bg-gradient-to-br shadow-lg ${gradient}`}
          >
            <LibraryIcon className="h-12 w-12 text-white/90" />
          </CardItem>
          <CardItem translateZ={25} as="p" className="truncate text-sm font-bold text-text-primary">
            {playlist.name}
          </CardItem>
          <CardItem translateZ={25} as="p" className="truncate text-xs text-text-secondary">
            {playlist.trackCount} track{playlist.trackCount === 1 ? "" : "s"}
            {ownerName ? ` · ${ownerName}` : ""}
          </CardItem>
        </CardBody>
      </CardItem>
    </CardContainer>
  );
}
