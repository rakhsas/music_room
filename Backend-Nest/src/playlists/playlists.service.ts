import { BadGatewayException, BadRequestException, ForbiddenException, forwardRef, Inject, Injectable, Logger, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { JamendoService } from '../music/jamendo.service';
import { User } from '../users/user.entity';
import { UsersService } from '../users/users.service';
import { assertPremium } from '../users/subscription';
import { Playlist } from './playlist.entity';
import { PlaylistsGateway } from './playlists.gateway';

const FREE_PLAYLIST_LIMIT = 3;

@Injectable()
export class PlaylistsService {
  private readonly logger = new Logger(PlaylistsService.name);

  constructor(
    @InjectRepository(Playlist) private readonly repo: Repository<Playlist>,
    private readonly users: UsersService,
    private readonly jamendo: JamendoService,
    @Inject(forwardRef(() => PlaylistsGateway)) private readonly gateway: PlaylistsGateway,
  ) {}

  private async load(id: number): Promise<Playlist> {
    const playlist = await this.repo.findOne({ where: { id }, relations: ['owner', 'collaborators', 'followers'] });
    if (!playlist) throw new NotFoundException('Playlist not found');
    return playlist;
  }

  // Same lost-update guard as EventsService.mutate: row lock, then load + save in one transaction.
  private mutate<T>(id: number, fn: (p: Playlist, save: (p: Playlist) => Promise<Playlist>) => Promise<T>): Promise<T> {
    return this.repo.manager.transaction(async (m) => {
      await m.findOne(Playlist, { where: { id }, lock: { mode: 'pessimistic_write' } });
      const playlist = await m.findOne(Playlist, { where: { id }, relations: ['owner', 'collaborators', 'followers'] });
      if (!playlist) throw new NotFoundException('Playlist not found');
      return fn(playlist, (p) => m.save(p));
    });
  }

  private isCollaborator(p: Playlist, userId: number) {
    return p.collaborators.some((c) => c.id === userId);
  }

  private isFollower(p: Playlist, userId: number) {
    return p.followers.some((f) => f.id === userId);
  }

  private canView(p: Playlist, userId?: number) {
    if (p.isPublic) return true;
    if (userId == null) return false;
    return p.ownerId === userId || this.isCollaborator(p, userId) || this.isFollower(p, userId);
  }

  canEdit(p: Playlist, userId: number) {
    return p.ownerId === userId || this.isCollaborator(p, userId) || p.couldEdit.includes(String(userId));
  }

  async canViewAsUserId(playlistId: number, userId: number) {
    const playlist = await this.repo.findOne({ where: { id: playlistId }, relations: ['collaborators', 'followers'] });
    return playlist ? this.canView(playlist, userId) : false;
  }

  async list() {
    const playlists = await this.repo.find({ where: { isPublic: true }, relations: ['owner', 'followers'], take: 20 });
    return playlists.map((p) => this.summarize(p));
  }

  async myPlaylists(user: User) {
    const owned = await this.repo.find({ where: { ownerId: user.id }, relations: ['owner', 'collaborators', 'followers'] });
    const collaborating = await this.repo
      .createQueryBuilder('p')
      .leftJoinAndSelect('p.owner', 'owner')
      .leftJoinAndSelect('p.collaborators', 'collaborators')
      .leftJoinAndSelect('p.followers', 'followers')
      .where('collaborators.id = :id', { id: user.id })
      .getMany();
    const following = await this.repo
      .createQueryBuilder('p')
      .leftJoinAndSelect('p.owner', 'owner')
      .leftJoinAndSelect('p.collaborators', 'collaborators')
      .leftJoinAndSelect('p.followers', 'followers')
      .where('followers.id = :id', { id: user.id })
      .getMany();

    const byId = new Map<number, Playlist>();
    for (const p of [...owned, ...collaborating, ...following]) byId.set(p.id, p);

    const playlists = [...byId.values()].map((p) => ({
      id: p.id,
      name: p.name,
      owner: { id: p.owner.id, name: p.owner.fullName, avatar: p.owner.avatar },
      trackCount: p.tracks.length,
      followersCount: p.followers.length,
      isPublic: p.isPublic,
      userRole: [
        ...(p.ownerId === user.id ? ['owner'] : []),
        ...(this.isCollaborator(p, user.id) ? ['collaborator'] : []),
        ...(this.isFollower(p, user.id) ? ['follower'] : []),
      ],
      canEdit: this.canEdit(p, user.id),
      createdAt: p.date,
    }));
    playlists.sort((a, b) => +new Date(b.createdAt) - +new Date(a.createdAt));
    return { playlists, count: playlists.length };
  }

  async ownedOnly(user: User) {
    const playlists = await this.repo.find({ where: { ownerId: user.id }, relations: ['followers', 'collaborators'] });
    const data = playlists.map((p) => ({
      id: p.id,
      name: p.name,
      trackCount: p.tracks.length,
      followersCount: p.followers.length,
      collaboratorsCount: p.collaborators.length,
      isPublic: p.isPublic,
      createdAt: p.date,
    }));
    return { playlists: data, count: data.length };
  }

  async collaborativeOnly(user: User) {
    const playlists = await this.repo
      .createQueryBuilder('p')
      .leftJoinAndSelect('p.owner', 'owner')
      .leftJoinAndSelect('p.collaborators', 'collaborators')
      .leftJoinAndSelect('p.followers', 'followers')
      .where('collaborators.id = :id', { id: user.id })
      .andWhere('p.ownerId != :id', { id: user.id })
      .getMany();
    const data = playlists.map((p) => ({
      id: p.id,
      name: p.name,
      owner: { id: p.owner.id, name: p.owner.fullName, avatar: p.owner.avatar },
      trackCount: p.tracks.length,
      followersCount: p.followers.length,
      isPublic: p.isPublic,
      canEdit: this.canEdit(p, user.id),
      createdAt: p.date,
    }));
    return { playlists: data, count: data.length };
  }

  async followedOnly(user: User) {
    const playlists = await this.repo
      .createQueryBuilder('p')
      .leftJoinAndSelect('p.owner', 'owner')
      .leftJoinAndSelect('p.followers', 'followers')
      .where('followers.id = :id', { id: user.id })
      .getMany();
    return playlists.map((p) => this.summarize(p));
  }

  private summarize(p: Playlist) {
    return {
      id: p.id,
      name: p.name,
      owner: p.owner.fullName,
      trackCount: p.tracks.length,
      followersCount: p.followers.length,
      createdAt: p.date,
    };
  }

  async detail(id: number, userId?: number) {
    const p = await this.load(id);
    if (!this.canView(p, userId)) throw new ForbiddenException('Access denied');
    return {
      id: p.id,
      name: p.name,
      owner: { id: p.owner.id, name: p.owner.fullName, avatar: p.owner.avatar },
      tracks: p.tracks,
      trackCount: p.tracks.length,
      isPublic: p.isPublic,
      followersCount: p.followers.length,
      collaborators: p.collaborators.map((c) => ({ id: c.id, name: c.fullName, avatar: c.avatar })),
      couldEdit: p.couldEdit,
      userPermissions: userId != null ? {
        canEdit: this.canEdit(p, userId),
        canDelete: p.ownerId === userId,
        isFollowing: this.isFollower(p, userId),
        isCollaborator: this.isCollaborator(p, userId),
        isOwner: p.ownerId === userId,
      } : {
        canEdit: false, canDelete: false, isFollowing: false, isCollaborator: false, isOwner: false,
      },
      createdAt: p.date,
    };
  }

  async create(owner: User, name: string, isPublic = true) {
    if (!name) throw new BadRequestException('Playlist name is required');
    if (!isPublic) assertPremium(owner, 'Private playlists');
    if (!owner.isPremium) {
      const owned = await this.repo.count({ where: { ownerId: owner.id } });
      if (owned >= FREE_PLAYLIST_LIMIT) {
        throw new ForbiddenException(
          `Free accounts can own up to ${FREE_PLAYLIST_LIMIT} playlists. Upgrade to premium for unlimited playlists.`,
        );
      }
    }
    const playlist = this.repo.create({ name, owner, ownerId: owner.id, isPublic, tracks: [], couldEdit: [], pendingInvites: [] });
    await this.repo.save(playlist);
    return { id: playlist.id, name: playlist.name, message: 'Playlist created successfully' };
  }

  async update(user: User, id: number, name?: string, isPublic?: boolean) {
    return this.mutate(id, async (p, save) => {
      if (!this.canEdit(p, user.id)) throw new ForbiddenException('Permission denied');
      if (name) p.name = name;
      // Gated on the owner, so a premium collaborator can't make a free user's playlist private.
      if (isPublic === false && p.isPublic) assertPremium(p.owner, 'Private playlists');
      if (isPublic !== undefined) p.isPublic = isPublic;
      await save(p);
      return { message: 'Playlist updated successfully' };
    });
  }

  async delete(user: User, id: number) {
    const p = await this.load(id);
    if (p.ownerId !== user.id) throw new ForbiddenException('Only playlist owner can delete');
    await this.repo.remove(p);
    return { message: 'Playlist deleted successfully' };
  }

  async follow(user: User, id: number) {
    return this.mutate(id, async (p, save) => {
      if (!p.isPublic && p.ownerId !== user.id) throw new ForbiddenException('Cannot follow private playlist');
      if (p.ownerId === user.id) throw new BadRequestException('Cannot follow your own playlist');
      if (this.isFollower(p, user.id)) return { message: 'Already following this playlist' };

      p.followers.push(user);
      if (!this.isCollaborator(p, user.id)) p.collaborators.push(user);
      await save(p);
      return { message: 'Playlist followed successfully', note: 'You now have edit permissions for this playlist' };
    });
  }

  async unfollow(user: User, id: number) {
    return this.mutate(id, async (p, save) => {
      if (!this.isFollower(p, user.id)) return { message: 'Not following this playlist' };

      p.followers = p.followers.filter((f) => f.id !== user.id);
      p.collaborators = p.collaborators.filter((c) => c.id !== user.id);
      await save(p);
      return { message: 'Playlist unfollowed successfully', note: 'Edit permissions have been removed' };
    });
  }

  async followersList(id: number) {
    const p = await this.load(id);
    return p.followers.map((f) => ({ id: f.id, name: f.fullName, avatar: f.avatar }));
  }

  async addCollaborator(actor: User, id: number, userId: number) {
    return this.mutate(id, async (p, save) => {
      if (p.ownerId !== actor.id) throw new ForbiddenException('Only playlist owner can add collaborators');
      const target = await this.requireUser(userId);
      if (target.id === p.ownerId) throw new BadRequestException('Owner is already a collaborator');
      if (this.isCollaborator(p, userId)) throw new BadRequestException(`User ${target.fullName} is already a collaborator`);

      p.collaborators.push(target);
      await save(p);
      return { message: `User ${target.fullName} added as collaborator` };
    });
  }

  async removeCollaborator(actor: User, id: number, userId: number) {
    return this.mutate(id, async (p, save) => {
      if (p.ownerId !== actor.id) throw new ForbiddenException('Only playlist owner can remove collaborators');
      const target = await this.requireUser(userId);
      if (!this.isCollaborator(p, userId)) return { message: 'User is not a collaborator' };

      p.collaborators = p.collaborators.filter((c) => c.id !== userId);
      await save(p);
      return { message: `User ${target.fullName} removed as collaborator` };
    });
  }

  // Broadcasts happen after mutate() resolves, i.e. after commit - otherwise listeners
  // refetch before the new track list is visible to them.
  async addTrack(user: User, id: number, trackId: string) {
    const tracks = await this.mutate(id, async (p, save) => {
      if (!this.canEdit(p, user.id)) throw new ForbiddenException('Permission denied');
      if (p.tracks.includes(String(trackId))) return null;
      p.tracks.push(String(trackId));
      return (await save(p)).tracks;
    });
    if (!tracks) return { message: 'Track already in playlist' };
    this.gateway.broadcast(id, 'track_added', { tracks, trackId: String(trackId) });
    return { message: 'Track added successfully' };
  }

  async removeTrack(user: User, id: number, trackId: string) {
    const tracks = await this.mutate(id, async (p, save) => {
      if (!this.canEdit(p, user.id)) throw new ForbiddenException('Permission denied');
      if (!p.tracks.includes(String(trackId))) return null;
      p.tracks = p.tracks.filter((t) => t !== String(trackId));
      return (await save(p)).tracks;
    });
    if (!tracks) return { message: 'Track not in playlist' };
    this.gateway.broadcast(id, 'track_removed', { tracks, trackId: String(trackId) });
    return { message: 'Track removed successfully' };
  }

  async getTracks(id: number, userId?: number) {
    const p = await this.load(id);
    if (!this.canView(p, userId)) throw new ForbiddenException('Access denied');
    const jamendoData = await this.jamendo.tracksByIds(p.tracks).catch((e) => {
      this.logger.error(`Jamendo tracksByIds failed for playlist ${id}: ${e?.message ?? e}`);
      throw new BadGatewayException('Failed to fetch track details from Jamendo');
    });
    return {
      playlistInfo: {
        id: p.id,
        name: p.name,
        owner: p.owner.fullName,
        trackCount: p.tracks.length,
        isPublic: p.isPublic,
        followersCount: p.followers.length,
      },
      // Jamendo returns its own order - re-sort so reorders actually show.
      tracks: (jamendoData.results ?? []).sort((a: any, b: any) => p.tracks.indexOf(String(a.id)) - p.tracks.indexOf(String(b.id))),
    };
  }

  async reorderTracks(user: User, id: number, order: string[]) {
    const tracks = await this.mutate(id, async (p, save) => {
      if (!this.canEdit(p, user.id)) throw new ForbiddenException('Permission denied');

      const same = order.length === p.tracks.length && [...order].sort().join() === [...p.tracks].sort().join();
      if (!same) throw new BadRequestException('Invalid track order');

      p.tracks = order;
      return (await save(p)).tracks;
    });
    this.gateway.broadcast(id, 'tracks_reordered', { tracks });
    return { message: 'Playlist tracks reordered successfully' };
  }

  async changeVisibility(user: User, id: number, isPublic: boolean) {
    return this.mutate(id, async (p, save) => {
      if (p.ownerId !== user.id) throw new ForbiddenException('Only playlist owner can change visibility');
      if (!isPublic && p.isPublic) assertPremium(p.owner, 'Private playlists');
      p.isPublic = isPublic;
      await save(p);
      return { message: `Playlist visibility changed to ${isPublic ? 'public' : 'private'}` };
    });
  }

  async grantEditPermission(actor: User, id: number, userId: number) {
    return this.mutate(id, async (p, save) => {
      if (p.ownerId !== actor.id) throw new ForbiddenException('Only playlist owner can grant edit permissions');
      const target = await this.requireUser(userId);
      if (p.couldEdit.includes(String(userId))) return { message: 'User already has edit permission' };

      p.couldEdit.push(String(userId));
      await save(p);
      return { message: `Edit permission granted to ${target.fullName}` };
    });
  }

  async revokeEditPermission(actor: User, id: number, userId: number) {
    return this.mutate(id, async (p, save) => {
      if (p.ownerId !== actor.id) throw new ForbiddenException('Only playlist owner can revoke edit permissions');
      const target = await this.requireUser(userId);
      if (!p.couldEdit.includes(String(userId))) return { message: 'User does not have edit permission' };

      p.couldEdit = p.couldEdit.filter((u) => u !== String(userId));
      await save(p);
      return { message: `Edit permission revoked from ${target.fullName}` };
    });
  }

  async invite(actor: User, id: number, username: string) {
    if (actor.userName === username) throw new BadRequestException('Cannot invite yourself to your own playlist');
    return this.mutate(id, async (p, save) => {
      if (p.isPublic) {
        throw new BadRequestException('Cannot invite users to public playlists. Public playlists can be followed directly.');
      }
      if (!this.canEdit(p, actor.id)) throw new ForbiddenException('Only owner or collaborators can invite users');
      const target = await this.requireUser(username);

      if (this.isCollaborator(p, target.id) || this.isFollower(p, target.id)) {
        throw new BadRequestException('User already has access to this playlist');
      }
      if (p.pendingInvites.includes(String(target.id))) throw new BadRequestException('User already has a pending invite');

      p.pendingInvites.push(String(target.id));
      await save(p);

      target.playlistNotifications = {
        ...target.playlistNotifications,
        [`${p.id}_${actor.id}`]: {
          playlistId: p.id,
          playlistName: p.name,
          inviterId: actor.id,
          inviterName: actor.fullName,
          invitedAt: new Date().toISOString(),
          message: `${actor.fullName} has invited you to ${p.name}`,
          note: 'You will get edit permissions when you accept',
        },
      };
      await this.users.save(target);
      return { message: 'Invitation sent successfully' };
    });
  }
  async acceptInvite(user: User, id: number) {
    return this.mutate(id, async (p, save) => {
      if (!p.pendingInvites.includes(String(user.id))) throw new BadRequestException('No pending invite found');

      if (!this.isCollaborator(p, user.id)) p.collaborators.push(user);
      if (!this.isFollower(p, user.id)) p.followers.push(user);
      p.pendingInvites = p.pendingInvites.filter((u) => u !== String(user.id));
      await save(p);
      await this.clearPlaylistNotification(user, p.id);

      return { message: 'Invitation accepted successfully' };
    });
  }

  async declineInvite(user: User, id: number) {
    return this.mutate(id, async (p, save) => {
      if (!p.pendingInvites.includes(String(user.id))) throw new BadRequestException('No pending invite found');

      p.pendingInvites = p.pendingInvites.filter((u) => u !== String(user.id));
      await save(p);
      await this.clearPlaylistNotification(user, p.id);

      return { message: 'Invitation declined' };
    });
  }

  private async clearPlaylistNotification(user: User, playlistId: number) {
    const notifications = { ...user.playlistNotifications };
    for (const key of Object.keys(notifications)) {
      if (key.startsWith(`${playlistId}_`)) delete notifications[key];
    }
    user.playlistNotifications = notifications;
    await this.users.save(user);
  }

  private async requireUser(identifier: string | number): Promise<User> {
    const user = typeof identifier === 'string' ? await this.users.findByUserName(identifier) : await this.users.findById(identifier);
    if (!user) throw new BadRequestException('User not found');
    return user;
  }
}
