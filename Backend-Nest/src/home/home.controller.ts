import { Controller, Get } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { User } from '../users/user.entity';
import { HomeService } from './home.service';
import { ApiDoc, JAMENDO_ARTIST, JAMENDO_TRACK } from '../utils/api-docs';

@ApiTags('home')
@ApiBearerAuth()
@Controller('home')
export class HomeController {
  constructor(private readonly home: HomeService) {}

  @Get()
  @ApiDoc(
    'Home screen feed',
    {
      userPlaylists: {
        results: [
          {
            id: 1,
            name: 'My Playlist',
            owner: { id: 1, name: 'John Doe', avatar: 'default_avatar.png' },
            trackCount: 4,
            followersCount: 2,
            isPublic: true,
            userRole: ['owner'],
            canEdit: true,
            createdAt: '2026-09-01T10:00:00.000Z',
          },
        ],
      },
      recommendedSongs: [JAMENDO_TRACK],
      recentlyListened: [JAMENDO_TRACK],
      popularSongs: [JAMENDO_TRACK],
      popularArtists: [JAMENDO_ARTIST],
      events: [
        {
          id: 3,
          title: 'Friday Night Jam',
          organizer: 'John Doe',
          location: 'E1',
          attendeeCount: 5,
          eventStartTime: '2026-09-20T20:00:00.000Z',
          isPublic: true,
        },
      ],
      notifications: {
        eventNotifications: [
          {
            eventId: 3,
            eventTitle: 'Friday Night Jam',
            inviterId: 2,
            inviterName: 'Jane Doe',
            invitedRole: 'attendee',
            type: 'event_invite',
            createdAt: '2026-09-18T12:00:00.000Z',
          },
        ],
        playlistNotifications: [
          {
            playlistId: 1,
            playlistName: 'My Playlist',
            inviterId: 2,
            inviterName: 'Jane Doe',
            invitedAt: '2026-09-18T12:00:00.000Z',
            message: 'Jane Doe has invited you to My Playlist',
            note: 'You will get edit permissions when you accept',
          },
        ],
      },
    },
    {
      description:
        'Aggregates the user\'s playlists, Jamendo recommendations (based on the user\'s `genres`), popular songs/artists, the 3 first public events and pending invites. Each section falls back to an empty list if its source fails. `recentlyListened` is random songs for now.',
    },
  )
  getHome(@CurrentUser() user: User) {
    return this.home.getHome(user);
  }
}
