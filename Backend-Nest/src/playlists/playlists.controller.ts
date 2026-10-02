import { Body, Controller, Delete, Get, Param, Post, Put } from '@nestjs/common';
import { ApiBearerAuth, ApiBody, ApiTags } from '@nestjs/swagger';
import { Public } from '../auth/decorators/public.decorator';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { User } from '../users/user.entity';
import { PlaylistsService } from './playlists.service';
import { CreatePlaylistDto } from './dto/create-playlist.dto';
import { VisibilityDto } from '../events/dto/visibility.dto';
import { UserIdDto } from './dto/user-id.dto';
import { ReorderTracksDto } from './dto/reorder-tracks.dto';
import { ApiDoc, JAMENDO_TRACK, msg } from '../utils/api-docs';

const NOT_FOUND = { 404: 'Playlist not found' };
const OWNER = { id: 1, name: 'John Doe', avatar: 'default_avatar.png' };
const SUMMARY = { id: 1, name: 'My Playlist', owner: 'John Doe', trackCount: 4, followersCount: 2, createdAt: '2026-09-01T10:00:00.000Z' };
const WS = 'Also broadcast to the playlist\'s websocket room.';

@ApiTags('playlists')
@ApiBearerAuth()
@Controller('playlists')
export class PlaylistsController {
  constructor(private readonly playlists: PlaylistsService) {}

  @Public()
  @Get()
  @ApiDoc('List public playlists (max 20)', [SUMMARY])
  list() {
    return this.playlists.list();
  }

  @Get('my')
  @ApiDoc(
    'Playlists I own, collaborate on or follow',
    {
      playlists: [
        {
          id: 1,
          name: 'My Playlist',
          owner: OWNER,
          trackCount: 4,
          followersCount: 2,
          isPublic: true,
          userRole: ['owner'],
          canEdit: true,
          createdAt: '2026-09-01T10:00:00.000Z',
        },
      ],
      count: 1,
    },
    { description: '`userRole` lists every relation I have: owner, collaborator, follower. Newest first.' },
  )
  mine(@CurrentUser() user: User) {
    return this.playlists.myPlaylists(user);
  }

  @Get('owned')
  @ApiDoc('Playlists I own', {
    playlists: [
      { id: 1, name: 'My Playlist', trackCount: 4, followersCount: 2, collaboratorsCount: 1, isPublic: true, createdAt: '2026-09-01T10:00:00.000Z' },
    ],
    count: 1,
  })
  owned(@CurrentUser() user: User) {
    return this.playlists.ownedOnly(user);
  }

  @Get('collaborative')
  @ApiDoc('Playlists I collaborate on (not owned)', {
    playlists: [
      { id: 5, name: 'Road Trip', owner: OWNER, trackCount: 10, followersCount: 3, isPublic: true, canEdit: true, createdAt: '2026-09-01T10:00:00.000Z' },
    ],
    count: 1,
  })
  collaborative(@CurrentUser() user: User) {
    return this.playlists.collaborativeOnly(user);
  }

  @Get('followed')
  @ApiDoc('Playlists I follow', [SUMMARY])
  followed(@CurrentUser() user: User) {
    return this.playlists.followedOnly(user);
  }

  @Post('create')
  @ApiDoc('Create a playlist', { id: 1, name: 'My Playlist', message: 'Playlist created successfully' }, {
    status: 201,
    errors: {
      400: 'Playlist name is required',
      403: 'Private playlists are a Premium feature. | Free accounts can own up to 3 playlists.',
    },
  })
  create(@CurrentUser() user: User, @Body() dto: CreatePlaylistDto) {
    return this.playlists.create(user, dto.name ?? '', dto.isPublic);
  }

  @Public()
  @Get(':playlistId')
  @ApiDoc(
    'Playlist detail',
    {
      id: 1,
      name: 'My Playlist',
      owner: OWNER,
      tracks: ['1204669', '1204670'],
      trackCount: 2,
      isPublic: true,
      followersCount: 2,
      collaborators: [{ id: 2, name: 'Jane Doe', avatar: 'default_avatar.png' }],
      couldEdit: ['3'],
      userPermissions: { canEdit: true, canDelete: true, isFollowing: false, isCollaborator: false, isOwner: true },
      createdAt: '2026-09-01T10:00:00.000Z',
    },
    {
      description: '`tracks` are Jamendo track ids in play order (use /tracks for full track objects). userPermissions are all false when anonymous.',
      errors: { 403: 'Access denied', ...NOT_FOUND },
    },
  )
  detail(@CurrentUser() user: User | undefined, @Param('playlistId') playlistId: string) {
    return this.playlists.detail(Number(playlistId), user?.id);
  }

  @Put(':playlistId/update')
  @ApiDoc('Rename / change visibility (editors)', msg('Playlist updated successfully'), {
    errors: { 403: 'Permission denied | Private playlists are a Premium feature.', ...NOT_FOUND },
  })
  update(@CurrentUser() user: User, @Param('playlistId') playlistId: string, @Body() dto: CreatePlaylistDto) {
    return this.playlists.update(user, Number(playlistId), dto.name, dto.isPublic);
  }

  @Delete(':playlistId/delete')
  @ApiDoc('Delete a playlist (owner only)', msg('Playlist deleted successfully'), {
    errors: { 403: 'Only playlist owner can delete', ...NOT_FOUND },
  })
  remove(@CurrentUser() user: User, @Param('playlistId') playlistId: string) {
    return this.playlists.delete(user, Number(playlistId));
  }

  @Post(':playlistId/follow')
  @ApiDoc('Follow a public playlist (also grants edit rights)', { message: 'Playlist followed successfully', note: 'You now have edit permissions for this playlist' }, {
    status: 201,
    description: 'Returns { message: "Already following this playlist" } if already following.',
    errors: { 400: 'Cannot follow your own playlist', 403: 'Cannot follow private playlist', ...NOT_FOUND },
  })
  follow(@CurrentUser() user: User, @Param('playlistId') playlistId: string) {
    return this.playlists.follow(user, Number(playlistId));
  }

  @Delete(':playlistId/unfollow')
  @ApiDoc('Unfollow (also removes edit rights)', { message: 'Playlist unfollowed successfully', note: 'Edit permissions have been removed' }, {
    description: 'Returns { message: "Not following this playlist" } if not following.',
    errors: NOT_FOUND,
  })
  unfollow(@CurrentUser() user: User, @Param('playlistId') playlistId: string) {
    return this.playlists.unfollow(user, Number(playlistId));
  }

  @Public()
  @Get(':playlistId/followers')
  @ApiDoc('List followers', [{ id: 2, name: 'Jane Doe', avatar: 'default_avatar.png' }], { errors: NOT_FOUND })
  followers(@Param('playlistId') playlistId: string) {
    return this.playlists.followersList(Number(playlistId));
  }

  @Post(':playlistId/collaborators/:userId/add')
  @ApiDoc('Add a collaborator (owner only)', msg('User Jane Doe added as collaborator'), {
    status: 201,
    errors: {
      400: 'User not found | Owner is already a collaborator | User Jane Doe is already a collaborator',
      403: 'Only playlist owner can add collaborators',
      ...NOT_FOUND,
    },
  })
  addCollaborator(@CurrentUser() user: User, @Param('playlistId') playlistId: string, @Param('userId') userId: string) {
    return this.playlists.addCollaborator(user, Number(playlistId), Number(userId));
  }

  @Delete(':playlistId/collaborators/:userId/remove')
  @ApiDoc('Remove a collaborator (owner only)', msg('User Jane Doe removed as collaborator'), {
    description: 'Returns "User is not a collaborator" if they weren\'t.',
    errors: { 400: 'User not found', 403: 'Only playlist owner can remove collaborators', ...NOT_FOUND },
  })
  removeCollaborator(@CurrentUser() user: User, @Param('playlistId') playlistId: string, @Param('userId') userId: string) {
    return this.playlists.removeCollaborator(user, Number(playlistId), Number(userId));
  }

  @Public()
  @Get(':playlistId/tracks')
  @ApiDoc(
    'Playlist tracks (full Jamendo objects, in play order)',
    {
      playlistInfo: { id: 1, name: 'My Playlist', owner: 'John Doe', trackCount: 1, isPublic: true, followersCount: 2 },
      tracks: [JAMENDO_TRACK],
    },
    { errors: { 403: 'Access denied', ...NOT_FOUND, 502: 'Failed to fetch track details from Jamendo' } },
  )
  tracks(@CurrentUser() user: User | undefined, @Param('playlistId') playlistId: string) {
    return this.playlists.getTracks(Number(playlistId), user?.id);
  }

  @Post(':playlistId/tracks/:trackId/add')
  @ApiDoc('Add a track (editors)', msg('Track added successfully'), {
    status: 201,
    description: `Returns "Track already in playlist" if present. ${WS}`,
    errors: { 403: 'Permission denied', ...NOT_FOUND },
  })
  addTrack(@CurrentUser() user: User, @Param('playlistId') playlistId: string, @Param('trackId') trackId: string) {
    return this.playlists.addTrack(user, Number(playlistId), trackId);
  }

  @Delete(':playlistId/tracks/:trackId/remove')
  @ApiDoc('Remove a track (editors)', msg('Track removed successfully'), {
    description: `Returns "Track not in playlist" if absent. ${WS}`,
    errors: { 403: 'Permission denied', ...NOT_FOUND },
  })
  removeTrack(@CurrentUser() user: User, @Param('playlistId') playlistId: string, @Param('trackId') trackId: string) {
    return this.playlists.removeTrack(user, Number(playlistId), trackId);
  }

  @Post(':playlistId/tracks/reorder')
  @ApiDoc('Reorder tracks (editors)', msg('Playlist tracks reordered successfully'), {
    status: 201,
    description: `trackOrder must contain exactly the current track ids. ${WS}`,
    errors: { 400: 'Invalid track order', 403: 'Permission denied', ...NOT_FOUND },
  })
  reorder(@CurrentUser() user: User, @Param('playlistId') playlistId: string, @Body() dto: ReorderTracksDto) {
    return this.playlists.reorderTracks(user, Number(playlistId), dto.trackOrder);
  }

  @Put(':playlistId/visibility')
  @ApiDoc('Make playlist public/private (owner only)', msg('Playlist visibility changed to private'), {
    errors: { 403: 'Only playlist owner can change visibility | Private playlists are a Premium feature.', ...NOT_FOUND },
  })
  visibility(@CurrentUser() user: User, @Param('playlistId') playlistId: string, @Body() dto: VisibilityDto) {
    return this.playlists.changeVisibility(user, Number(playlistId), dto.isPublic);
  }

  @Post(':playlistId/permissions/grant')
  @ApiDoc('Grant edit permission (owner only)', msg('Edit permission granted to Jane Doe'), {
    status: 201,
    description: 'Returns "User already has edit permission" if already granted.',
    errors: { 400: 'User not found', 403: 'Only playlist owner can grant edit permissions', ...NOT_FOUND },
  })
  grant(@CurrentUser() user: User, @Param('playlistId') playlistId: string, @Body() dto: UserIdDto) {
    return this.playlists.grantEditPermission(user, Number(playlistId), dto.userId);
  }

  @Delete(':playlistId/permissions/revoke/:userId')
  @ApiDoc('Revoke edit permission (owner only)', msg('Edit permission revoked from Jane Doe'), {
    description: 'Returns "User does not have edit permission" if not granted.',
    errors: { 400: 'User not found', 403: 'Only playlist owner can revoke edit permissions', ...NOT_FOUND },
  })
  revoke(@CurrentUser() user: User, @Param('playlistId') playlistId: string, @Param('userId') userId: string) {
    return this.playlists.revokeEditPermission(user, Number(playlistId), Number(userId));
  }

  @Post(':playlistId/invite')
  @ApiBody({ schema: { example: { username: 'janedoe' } } })
  @ApiDoc('Invite a user to a private playlist (editors)', msg('Invitation sent successfully'), {
    status: 201,
    description: 'The invitee sees it in /api/home notifications and gets edit rights on accept.',
    errors: {
      400: 'Cannot invite users to public playlists. | Cannot invite yourself | User not found | User already has access to this playlist | User already has a pending invite',
      403: 'Only owner or collaborators can invite users',
      ...NOT_FOUND,
    },
  })
  invite(@CurrentUser() user: User, @Param('playlistId') playlistId: string, @Body() userNameDto: { username: string }) {
    return this.playlists.invite(user, Number(playlistId), userNameDto.username);
  }

  @Post(':playlistId/accept-invite')
  @ApiDoc('Accept my invite (become collaborator + follower)', msg('Invitation accepted successfully'), {
    status: 201,
    errors: { 400: 'No pending invite found', ...NOT_FOUND },
  })
  acceptInvite(@CurrentUser() user: User, @Param('playlistId') playlistId: string) {
    return this.playlists.acceptInvite(user, Number(playlistId));
  }

  @Post(':playlistId/decline-invite')
  @ApiDoc('Decline my invite', msg('Invitation declined'), {
    status: 201,
    errors: { 400: 'No pending invite found', ...NOT_FOUND },
  })
  declineInvite(@CurrentUser() user: User, @Param('playlistId') playlistId: string) {
    return this.playlists.declineInvite(user, Number(playlistId));
  }
}
