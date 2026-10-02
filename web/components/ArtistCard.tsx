import Image from "next/image";
import Link from "next/link";
import type { JamendoArtist } from "@/lib/types";
import { CardBody, CardContainer, CardItem } from "./ui/3d-card";

export function ArtistCard({ artist }: { artist: JamendoArtist }) {
  return (
    <CardContainer containerClassName="w-48 shrink-0 py-0">
      <CardItem as={Link} href={`/artists/${artist.id}`} translateZ={0} className="block">
        <CardBody className="w-48 rounded-2xl border border-white/5 bg-surface p-4 text-center shadow-xl transition-colors hover:bg-card">
          <CardItem translateZ={50} className="mx-auto mb-4 h-40 w-40 overflow-hidden rounded-full shadow-lg ring-2 ring-white/10">
            <div className="relative h-full w-full">
              {artist.image && <Image src={artist.image} alt="" fill sizes="160px" className="object-cover" unoptimized />}
            </div>
          </CardItem>
          <CardItem translateZ={25} as="p" className="truncate text-sm font-bold text-text-primary">
            {artist.name}
          </CardItem>
          <CardItem translateZ={25} as="p" className="text-xs text-text-secondary">
            Artist
          </CardItem>
        </CardBody>
      </CardItem>
    </CardContainer>
  );
}
