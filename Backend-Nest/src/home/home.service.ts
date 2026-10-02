import { Injectable } from '@nestjs/common';
import { EventsService } from '../events/events.service';
import { JamendoService } from '../music/jamendo.service';
import { PlaylistsService } from '../playlists/playlists.service';
import { User } from '../users/user.entity';

@Injectable()
export class HomeService {
  constructor(
    private readonly playlists: PlaylistsService,
    private readonly jamendo: JamendoService,
    private readonly events: EventsService,
  ) {}

  async getHome(user: User) {
    const [userPlaylists, recommendedSongs, popularSongs, popularArtists, events] = await Promise.all([
      this.playlists.myPlaylists(user).catch(() => ({ playlists: [], count: 0 })),
      this.jamendo.relatedForGenres(user.genres, 5).catch(() => ({ results: [] })),
      this.jamendo.randomSongs(5).catch(() => ({ results: [] })),
      this.jamendo.popularArtists(5).catch(() => ({ results: [] })),
      this.events.list().catch(() => []),
    ]);

    return {
      userPlaylists: { results: userPlaylists.playlists },
      recommendedSongs: recommendedSongs.results ?? [],
      // No "recently played" tracking exists yet - random stands in until it does.
      recentlyListened: popularSongs.results ?? [],
      popularSongs: popularSongs.results ?? [],
      popularArtists: popularArtists.results ?? [],
      events: events.slice(0, 3),
      notifications: {
        eventNotifications: Object.values(user.eventNotifications ?? {}),
        playlistNotifications: Object.values(user.playlistNotifications ?? {}),
      },
    };
  }
}
