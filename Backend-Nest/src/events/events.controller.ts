import { Body, Controller, Delete, Get, Param, Post, Put, Query } from '@nestjs/common';
import { ApiBearerAuth, ApiQuery, ApiTags } from '@nestjs/swagger';
import { Public } from '../auth/decorators/public.decorator';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { User } from '../users/user.entity';
import { EventsService } from './events.service';
import { CreateEventDto } from './dto/create-event.dto';
import { ChangeVoteLicenseDto } from './dto/vote-license.dto';
import { VisibilityDto } from './dto/visibility.dto';
import { InviteUserDto } from './dto/invite-user.dto';
import { LOCATION_CHOICES } from './event.entity';
import { ApiDoc, JAMENDO_TRACK, msg } from '../utils/api-docs';

const NOT_FOUND = { 404: 'Event not found' };
const NO_ACCESS = { 403: 'Access denied', ...NOT_FOUND };
const USER_NOT_FOUND = { 404: 'Event not found | User not found' };

@ApiTags('events')
@ApiBearerAuth()
@Controller('events')
export class EventsController {
  constructor(private readonly events: EventsService) {}

  @Public()
  @Get()
  @ApiQuery({ name: 'location', required: false, enum: LOCATION_CHOICES })
  @ApiDoc(
    'List public events (max 20)',
    [
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
    { errors: { 400: 'Invalid location filter. Must be one of: E1, E2, ...' } },
  )
  list(@Query('location') location?: string) {
    return this.events.list(location);
  }

  @Public()
  @Get('locations')
  @ApiDoc('Allowed event locations', { locations: [{ value: 'E1', label: 'E1' }, { value: 'Agora', label: 'Agora' }] })
  locations() {
    return { locations: this.events.getLocations() };
  }

  @Post('create')
  @ApiDoc('Create an event (I become owner)', { id: 3, title: 'Friday Night Jam', message: 'Event created successfully' }, {
    status: 201,
    errors: {
      400: 'Invalid location. Must be one of: ...',
      403: 'Private events are a Premium feature. | Free accounts can organize up to 3 events.',
    },
  })
  create(@CurrentUser() user: User, @Body() dto: CreateEventDto) {
    return this.events.create(user, dto);
  }

  @Get('my-events')
  @ApiDoc(
    'Events I organize, manage or attend',
    {
      events: [
        {
          id: 3,
          title: 'Friday Night Jam',
          description: 'Come vote for the next tracks!',
          location: 'E1',
          startTime: '2026-09-20T20:00:00.000Z',
          endTime: '2026-09-20T23:00:00.000Z',
          isPublic: true,
          organizer: 'John Doe',
          createdAt: '2026-09-01T10:00:00.000Z',
          updatedAt: '2026-09-01T10:00:00.000Z',
          attendeesCount: 5,
          trackCount: 12,
          userRole: 'owner',
          canEdit: true,
        },
      ],
      count: 1,
    },
    { description: '`userRole`: owner | editor | listener (or organizer | manager | attendee for legacy rows).' },
  )
  myEvents(@CurrentUser() user: User) {
    return this.events.myEvents(user.id);
  }

  @Public()
  @Get(':eventId')
  @ApiDoc(
    'Event detail',
    {
      id: 3,
      title: 'Friday Night Jam',
      description: 'Come vote for the next tracks!',
      location: 'E1',
      eventStartTime: '2026-09-20T20:00:00.000Z',
      eventEndTime: '2026-09-20T23:00:00.000Z',
      organizer: { id: 1, name: 'John Doe', avatar: 'default_avatar.png' },
      attendeeCount: 5,
      trackCount: 1,
      isPublic: true,
      songs: [{ trackId: '1204669', voteCount: 3, hasUserVoted: true }],
      userRoles: { '1': 'owner', '2': 'listener' },
      currentUserRole: 'owner',
    },
    { description: 'Private events need a token of the organizer or an attendee. `currentUserRole` is null when anonymous.', errors: NO_ACCESS },
  )
  async detail(@CurrentUser() user: User | undefined, @Param('eventId') eventId: string) {
    var result = await this.events.detail(Number(eventId), user?.id);
    console.log(`Detail for event ${eventId} requested by user ${user?.id ?? 'unauthenticated'}. Result:`, result);
    return result;
  }

  @Post(':eventId/join')
  @ApiDoc('Join a public event (as listener)', msg('Successfully joined event'), {
    status: 201,
    description: 'Also returns 201 with "Already attending this event" if already in.',
    errors: { 403: 'Cannot join private event', ...NOT_FOUND },
  })
  join(@CurrentUser() user: User, @Param('eventId') eventId: string) {
    return this.events.join(user, Number(eventId));
  }

  @Delete(':eventId/leave')
  @ApiDoc('Leave an event', msg('Successfully left event'), {
    description: 'Returns "Not attending this event" if not in.',
    errors: { 400: 'Organizer cannot leave their own event', ...NOT_FOUND },
  })
  leave(@CurrentUser() user: User, @Param('eventId') eventId: string) {
    return this.events.leave(user, Number(eventId));
  }

  @Post(':eventId/add-user/:userId')
  @ApiDoc('Add a user as attendee (owner/editor)', msg('User Jane Doe added successfully'), {
    status: 201,
    errors: { 403: 'Only organizer or managers can add users', ...USER_NOT_FOUND },
  })
  addUser(@CurrentUser() user: User, @Param('eventId') eventId: string, @Param('userId') userId: string) {
    return this.events.addUser(user, Number(eventId), Number(userId));
  }

  @Delete(':eventId/remove-user/:userId')
  @ApiDoc('Remove an attendee (owner/editor)', msg('User Jane Doe removed successfully'), {
    errors: { 400: 'Cannot remove organizer from event', 403: 'Only organizer or managers can remove users', ...USER_NOT_FOUND },
  })
  removeUser(@CurrentUser() user: User, @Param('eventId') eventId: string, @Param('userId') userId: string) {
    return this.events.removeUser(user, Number(eventId), Number(userId));
  }

  @Public()
  @Get(':eventId/attendees')
  @ApiDoc('List attendees', [{ id: 2, name: 'Jane Doe', avatar: 'default_avatar.png' }], { errors: NO_ACCESS })
  attendees(@CurrentUser() user: User | undefined, @Param('eventId') eventId: string) {
    return this.events.attendeesList(user, Number(eventId));
  }

  @Public()
  @Get(':eventId/tracks')
  @ApiDoc(
    'Event tracks with votes, most voted first',
    {
      eventInfo: { id: 3, title: 'Friday Night Jam', organizer: 'John Doe', trackCount: 1, attendeeCount: 5, isPublic: true },
      tracks: [{ ...JAMENDO_TRACK, votes: 3 }],
    },
    { description: 'Each track is the Jamendo track object plus `votes`.', errors: { ...NO_ACCESS, 502: 'Failed to fetch track details from Jamendo' } },
  )
  async tracks(@CurrentUser() user: User | undefined, @Param('eventId') eventId: string) {
    const result = await this.events.getTracks(user, Number(eventId));
    console.log(`Tracks for event ${eventId} requested by user ${user?.id ?? 'unauthenticated'}. Result:`, result);
    console.log("----------------------------------")
    return result;
  }

  @Post(':eventId/tracks/:trackId/add')
  @ApiDoc('Suggest a track (attendees)', msg('Track added successfully'), {
    status: 201,
    description: 'Returns "Track already in event" if present.',
    errors: { 403: 'Only attendees can add tracks', ...NOT_FOUND },
  })
  addTrack(@CurrentUser() user: User, @Param('eventId') eventId: string, @Param('trackId') trackId: string) {
    return this.events.addTrack(user, Number(eventId), trackId);
  }

  @Delete(':eventId/tracks/:trackId/remove')
  @ApiDoc('Remove a track and its votes (owner/editor)', msg('Track removed successfully'), {
    description: 'Returns "Track not in event" if absent.',
    errors: { 403: 'Only organizer or managers can remove tracks', ...NOT_FOUND },
  })
  removeTrack(@CurrentUser() user: User, @Param('eventId') eventId: string, @Param('trackId') trackId: string) {
    return this.events.removeTrack(user, Number(eventId), trackId);
  }

  @Post(':eventId/tracks/:trackId/vote')
  @ApiDoc('Vote for a track', msg('Vote added successfully'), {
    status: 201,
    description: 'Returns "Already voted for this track or track not in event" when nothing changed. Who may vote depends on the vote license.',
    errors: {
      403: 'Access denied | Only invited attendees can vote on this event | Voting is only open between 16:00 and 18:00',
      ...NOT_FOUND,
    },
  })
  vote(@CurrentUser() user: User, @Param('eventId') eventId: string, @Param('trackId') trackId: string) {
    return this.events.voteTrack(user, Number(eventId), trackId);
  }

  @Delete(':eventId/tracks/:trackId/unvote')
  @ApiDoc('Remove my vote', msg('Vote removed successfully'), {
    description: 'Returns "Vote not found" if I had not voted.',
    errors: { 403: 'Access denied | Only invited attendees can vote on this event', ...NOT_FOUND },
  })
  unvote(@CurrentUser() user: User, @Param('eventId') eventId: string, @Param('trackId') trackId: string) {
    return this.events.unvoteTrack(user, Number(eventId), trackId);
  }

  @Put(':eventId/visibility')
  @ApiDoc('Make event public/private (owner/editor)', msg('Event visibility changed to private'), {
    errors: { 403: 'Only organizer or managers can change visibility | Private events are a Premium feature.', ...NOT_FOUND },
  })
  visibility(@CurrentUser() user: User, @Param('eventId') eventId: string, @Body() dto: VisibilityDto) {
    return this.events.changeVisibility(user, Number(eventId), dto.isPublic);
  }

  @Put(':eventId/vote-license')
  @ApiDoc('Change who can vote (owner/editor)', msg('Vote license changed to time_window'), {
    description: 'everyone | invited_only (attendees) | time_window (attendees, between voteWindowStart and voteWindowEnd HH:MM).',
    errors: {
      400: 'vote_window_start and vote_window_end (HH:MM) are required for the time_window license',
      403: 'Only organizer or managers can change the vote license',
      ...NOT_FOUND,
    },
  })
  voteLicense(@CurrentUser() user: User, @Param('eventId') eventId: string, @Body() dto: ChangeVoteLicenseDto) {
    return this.events.changeVoteLicense(user, Number(eventId), dto.voteLicense, dto.voteWindowStart, dto.voteWindowEnd);
  }

  @Get(':eventId/roles')
  @ApiDoc(
    'Roles of everyone in the event',
    {
      userRoles: { '1': 'owner', '2': 'listener' },
      usersByRole: {
        owner: [{ id: 1, name: 'John Doe', avatar: 'default_avatar.png', role: 'owner' }],
        editor: [],
        listener: [{ id: 2, name: 'Jane Doe', avatar: 'default_avatar.png', role: 'listener' }],
      },
    },
    { errors: NO_ACCESS },
  )
  roles(@CurrentUser() user: User, @Param('eventId') eventId: string) {
    return this.events.getUserRoles(user, Number(eventId));
  }

  @Post(':eventId/assign-editor/:userId')
  @ApiDoc('Make a user editor (owner only)', msg('User Jane Doe assigned as editor successfully'), {
    status: 201,
    errors: { 403: 'Only event owner can assign editor roles', ...USER_NOT_FOUND },
  })
  assignEditor(@CurrentUser() user: User, @Param('eventId') eventId: string, @Param('userId') userId: string) {
    return this.events.assignEditorRole(user, Number(eventId), Number(userId));
  }

  @Post(':eventId/transfer-ownership/:userId')
  @ApiDoc('Transfer ownership (I become editor)', msg('Ownership transferred to Jane Doe successfully'), {
    status: 201,
    errors: { 403: 'Only current owner can transfer ownership', ...USER_NOT_FOUND },
  })
  transferOwnership(@CurrentUser() user: User, @Param('eventId') eventId: string, @Param('userId') userId: string) {
    return this.events.transferOwnership(user, Number(eventId), Number(userId));
  }

  @Delete(':eventId/remove-user-role/:userId')
  @ApiDoc('Remove a user from the event (owner, or yourself)', msg('User Jane Doe removed from event successfully'), {
    errors: {
      400: 'Cannot remove the event owner | User is not part of this event',
      403: 'Only owner or the user themselves can remove from event',
      ...USER_NOT_FOUND,
    },
  })
  removeUserRole(@CurrentUser() user: User, @Param('eventId') eventId: string, @Param('userId') userId: string) {
    return this.events.removeUserRole(user, Number(eventId), Number(userId));
  }

  @Post(':eventId/invite')
  @ApiDoc('Invite a user', msg('Invitation sent successfully for attendee role'), {
    status: 201,
    description: 'Owner can invite any role; managers only attendees. The invitee sees it in /api/home notifications.',
    errors: {
      400: 'User is already attending this event | User already has a pending invite',
      403: "You don't have permission to invite users as manager",
      ...USER_NOT_FOUND,
    },
  })
  invite(@CurrentUser() user: User, @Param('eventId') eventId: string, @Body() dto: InviteUserDto) {
    return this.events.invite(user, Number(eventId), dto.userId, dto.role ?? 'attendee');
  }

  @Get(':eventId/pending-invites')
  @ApiDoc(
    'Pending invites (owner only)',
    {
      pendingInvites: [{ userId: '2', name: 'Jane Doe', email: 'jane@example.com', role: 'attendee', invitedAt: '2026-09-18T12:00:00.000Z' }],
      count: 1,
    },
    { errors: { 403: 'Only organizers and managers can view pending invites', ...NOT_FOUND } },
  )
  pendingInvites(@CurrentUser() user: User, @Param('eventId') eventId: string) {
    return this.events.pendingInvitesFor(user, Number(eventId));
  }

  @Post(':eventId/accept-invite')
  @ApiDoc('Accept my invite', msg('Invitation accepted successfully. You are now a attendee'), {
    status: 201,
    errors: { 400: 'No pending invite found', ...NOT_FOUND },
  })
  acceptInvite(@CurrentUser() user: User, @Param('eventId') eventId: string) {
    return this.events.acceptInvite(user, Number(eventId));
  }

  @Post(':eventId/decline-invite')
  @ApiDoc('Decline my invite', msg('Invitation declined'), {
    status: 201,
    errors: { 400: 'No pending invite found', ...NOT_FOUND },
  })
  declineInvite(@CurrentUser() user: User, @Param('eventId') eventId: string) {
    return this.events.declineInvite(user, Number(eventId));
  }
}
