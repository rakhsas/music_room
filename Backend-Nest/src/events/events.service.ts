import { BadGatewayException, BadRequestException, ForbiddenException, Injectable, Logger, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { JamendoService } from '../music/jamendo.service';
import { User } from '../users/user.entity';
import { UsersService } from '../users/users.service';
import { assertPremium } from '../users/subscription';
import { Events, EventRole, LOCATION_CHOICES, VOTE_LICENSE_CHOICES } from './event.entity';
import { CreateEventDto } from './dto/create-event.dto';

const FREE_EVENT_LIMIT = 3;

const ROLE_PERMISSIONS: Record<EventRole, string[]> = {
  owner: ['edit_event', 'delete_event', 'add_tracks', 'remove_tracks', 'manage_users', 'invite_users', 'invite_organizers', 'invite_managers', 'invite_attendees'],
  editor: ['edit_event', 'add_tracks', 'remove_tracks', 'invite_attendees'],
  listener: ['vote_tracks'],
};

@Injectable()
export class EventsService {
  private readonly logger = new Logger(EventsService.name);

  constructor(
    @InjectRepository(Events) private readonly repo: Repository<Events>,
    private readonly users: UsersService,
    private readonly jamendo: JamendoService,
  ) {}

  private async load(id: number): Promise<Events> {
    const event = await this.repo.findOne({ where: { id }, relations: ['organizer', 'attendees'] });
    if (!event) throw new NotFoundException('Event not found');
    return event;
  }

  // Every write is load -> mutate jsonb/relations in memory -> save, so two concurrent
  // votes/joins on one event would otherwise overwrite each other (lost update - seen
  // in the JMeter run). Row lock first (no joins: Postgres refuses FOR UPDATE on the
  // nullable side of an outer join), then load with relations inside the same transaction.
  private mutate<T>(id: number, fn: (event: Events, save: (e: Events) => Promise<Events>) => Promise<T>): Promise<T> {
    return this.repo.manager.transaction(async (m) => {
      await m.findOne(Events, { where: { id }, lock: { mode: 'pessimistic_write' } });
      const event = await m.findOne(Events, { where: { id }, relations: ['organizer', 'attendees'] });
      if (!event) throw new NotFoundException('Event not found');
      return fn(event, (e) => m.save(e));
    });
  }

  private isOrganizer(event: Events, userId: number) {
    return event.organizerId === userId;
  }

  private isManager(event: Events, userId: number) {
    return event.managers.includes(String(userId));
  }

  private isAttendee(event: Events, userId: number) {
    return event.attendees.some((a) => a.id === userId);
  }

  private hasPermission(event: Events, userId: number, action: string) {
    if (this.isOrganizer(event, userId)) return ROLE_PERMISSIONS.owner.includes(action);
    if (this.isManager(event, userId)) return ROLE_PERMISSIONS.editor.includes(action);
    const role = event.userRoles[String(userId)];
    if (this.isAttendee(event, userId)) {
      return ROLE_PERMISSIONS[role ?? 'listener']?.includes(action) ?? false;
    }
    return false;
  }

  private canView(event: Events, userId?: number) {
    if (event.isPublic) return true;
    if (userId == null) return false;
    return this.isOrganizer(event, userId) || this.isAttendee(event, userId);
  }

  private canEdit(event: Events, userId: number) {
    return this.hasPermission(event, userId, 'edit_event');
  }

  private assignRole(event: Events, userId: number, role: EventRole) {
    event.userRoles[String(userId)] = role;
  }

  getUserRole(event: Events, userId: number) {
    return event.userRoles[String(userId)] ?? null;
  }

  getLocations() {
    return LOCATION_CHOICES.map((value) => ({ value, label: value }));
  }

  async list(location?: string) {
    if (location && !LOCATION_CHOICES.includes(location as any)) {
      throw new BadRequestException(`Invalid location filter. Must be one of: ${LOCATION_CHOICES.join(', ')}`);
    }
    const qb = this.repo
      .createQueryBuilder('e')
      .leftJoinAndSelect('e.organizer', 'organizer')
      .leftJoinAndSelect('e.attendees', 'attendees')
      .where('e.isPublic = true')
      .take(20);
    if (location) qb.andWhere('e.location = :location', { location });
    const events = await qb.getMany();
    return events.map((e) => ({
      id: e.id,
      title: e.title,
      organizer: e.organizer.fullName,
      location: e.location,
      attendeeCount: e.attendees.length,
      eventStartTime: e.eventStartTime,
      isPublic: e.isPublic,
    }));
  }

  async create(organizer: User, dto: CreateEventDto) {
    if (!LOCATION_CHOICES.includes(dto.location as any)) {
      throw new BadRequestException(`Invalid location. Must be one of: ${LOCATION_CHOICES.join(', ')}`);
    }
    if (dto.isPublic === false) assertPremium(organizer, 'Private events');
    if (!organizer.isPremium) {
      const owned = await this.repo.count({ where: { organizerId: organizer.id } });
      if (owned >= FREE_EVENT_LIMIT) {
        throw new ForbiddenException(
          `Free accounts can organize up to ${FREE_EVENT_LIMIT} events. Upgrade to premium for unlimited events.`,
        );
      }
    }
    const event = this.repo.create({
      title: dto.title,
      description: dto.description ?? '',
      location: dto.location,
      eventStartTime: new Date(dto.eventStartTime),
      eventEndTime: dto.eventEndTime ? new Date(dto.eventEndTime) : null,
      organizer,
      organizerId: organizer.id,
      isPublic: dto.isPublic ?? true,
      attendees: [],
      userRoles: { [String(organizer.id)]: 'owner' },
    });
    await this.repo.save(event);
    return { id: event.id, title: event.title, message: 'Event created successfully' };
  }

  async detail(id: number, userId?: number) {
    const event = await this.load(id);
    if (!this.canView(event, userId)) throw new ForbiddenException('Access denied');

    const songsWithVotes = event.songs.map((trackId) => ({
      trackId,
      voteCount: (event.trackVotes[trackId] ?? []).length,
      hasUserVoted: userId != null ? (event.trackVotes[trackId] ?? []).includes(String(userId)) : false,
    }));

    return {
      id: event.id,
      title: event.title,
      description: event.description,
      location: event.location,
      eventStartTime: event.eventStartTime,
      eventEndTime: event.eventEndTime,
      organizer: { id: event.organizer.id, name: event.organizer.fullName, avatar: event.organizer.avatar },
      attendeeCount: event.attendees.length,
      trackCount: event.songs.length,
      isPublic: event.isPublic,
      songs: songsWithVotes,
      userRoles: event.userRoles,
      currentUserRole: userId != null ? this.getUserRole(event, userId) : null,
    };
  }

  async join(user: User, id: number) {
    return this.mutate(id, async (event, save) => {
      if (!event.isPublic && event.organizerId !== user.id) {
        throw new ForbiddenException('Cannot join private event');
      }
      if (this.isAttendee(event, user.id)) return { message: 'Already attending this event' };

      event.attendees.push(user);
      this.assignRole(event, user.id, 'listener');
      await save(event);
      return { message: 'Successfully joined event' };
    });
  }

  async leave(user: User, id: number) {
    return this.mutate(id, async (event, save) => {
      if (event.organizerId === user.id) throw new BadRequestException('Organizer cannot leave their own event');
      if (!this.isAttendee(event, user.id)) return { message: 'Not attending this event' };

      event.attendees = event.attendees.filter((a) => a.id !== user.id);
      delete event.userRoles[String(user.id)];
      await save(event);
      return { message: 'Successfully left event' };
    });
  }

  async addUser(actor: User, id: number, userId: number) {
    return this.mutate(id, async (event, save) => {
      const userToAdd = await this.requireUser(userId);
      if (!this.canEdit(event, actor.id)) throw new ForbiddenException('Only organizer or managers can add users');
      if (this.isAttendee(event, userId)) return { message: 'User already attending this event' };

      event.attendees.push(userToAdd);
      this.assignRole(event, userId, 'listener');
      await save(event);
      return { message: `User ${userToAdd.fullName} added successfully` };
    });
  }

  async removeUser(actor: User, id: number, userId: number) {
    return this.mutate(id, async (event, save) => {
      const userToRemove = await this.requireUser(userId);
      if (!this.canEdit(event, actor.id)) throw new ForbiddenException('Only organizer or managers can remove users');
      if (userToRemove.id === event.organizerId) throw new BadRequestException('Cannot remove organizer from event');
      if (!this.isAttendee(event, userId)) return { message: 'User not attending this event' };

      event.attendees = event.attendees.filter((a) => a.id !== userId);
      delete event.userRoles[String(userId)];
      await save(event);
      return { message: `User ${userToRemove.fullName} removed successfully` };
    });
  }

  async attendeesList(actor: User | undefined, id: number) {
    const event = await this.load(id);
    if (!this.canView(event, actor?.id)) throw new ForbiddenException('Access denied');
    return event.attendees.map((a) => ({ id: a.id, name: a.fullName, avatar: a.avatar }));
  }

  async addTrack(user: User, id: number, trackId: string) {
    return this.mutate(id, async (event, save) => {
      if (!(event.organizerId === user.id || this.isAttendee(event, user.id))) {
        throw new ForbiddenException('Only attendees can add tracks');
      }
      if (event.songs.includes(String(trackId))) return { message: 'Track already in event' };

      event.songs.push(String(trackId));
      await save(event);
      return { message: 'Track added successfully' };
    });
  }

  async removeTrack(user: User, id: number, trackId: string) {
    return this.mutate(id, async (event, save) => {
      if (!this.canEdit(event, user.id)) throw new ForbiddenException('Only organizer or managers can remove tracks');
      if (!event.songs.includes(String(trackId))) return { message: 'Track not in event' };

      event.songs = event.songs.filter((t) => t !== String(trackId));
      delete event.trackVotes[String(trackId)];
      await save(event);
      return { message: 'Track removed successfully' };
    });
  }

  async getTracks(actor: User | undefined, id: number) {
    const event = await this.load(id);
    if (!this.canView(event, actor?.id)) throw new ForbiddenException('Access denied');

    const jamendoData = await this.jamendo.tracksByIds(event.songs).catch((e) => {
      // Swallowing this used to return `{results: []}`, which looked exactly like "this event
      // has no tracks" to the caller and silently wiped out the track list in the app on every
      // transient Jamendo hiccup (e.g. right after a vote/unvote triggers a refetch). Surface it
      // as a real failure instead so the app keeps showing the last-known track list.
      this.logger.error(`Jamendo tracksByIds failed for event ${id}: ${e?.message ?? e}`);
      throw new BadGatewayException('Failed to fetch track details from Jamendo');
    });
    const tracks = (jamendoData.results ?? []).map((track: any) => ({
      ...track,
      votes: (event.trackVotes[String(track.id)] ?? []).length,
    }));
    tracks.sort((a: any, b: any) => b.votes - a.votes);

    return {
      eventInfo: {
        id: event.id,
        title: event.title,
        organizer: event.organizer.fullName,
        trackCount: event.songs.length,
        attendeeCount: event.attendees.length,
        isPublic: event.isPublic,
      },
      tracks,
    };
  }

  private canVote(event: Events, userId: number): [boolean, string?] {
    if (this.isOrganizer(event, userId) || this.isManager(event, userId)) return [true];
    if (!event.isPublic && !this.isAttendee(event, userId)) {
      return [false, 'This event is private - only invited attendees can vote'];
    }
    if (event.voteLicense === 'invited_only' && !this.isAttendee(event, userId)) {
      return [false, 'Only invited attendees can vote on this event'];
    }
    if (event.voteLicense === 'time_window') {
      if (!this.isAttendee(event, userId)) return [false, 'Only invited attendees can vote on this event'];
      if (event.voteWindowStart && event.voteWindowEnd) {
        const now = new Date().toTimeString().slice(0, 5);
        const inWindow =
          event.voteWindowStart <= event.voteWindowEnd
            ? now >= event.voteWindowStart && now <= event.voteWindowEnd
            : now >= event.voteWindowStart || now <= event.voteWindowEnd;
        if (!inWindow) {
          return [false, `Voting is only open between ${event.voteWindowStart} and ${event.voteWindowEnd}`];
        }
      }
    }
    return [true];
  }

  async voteTrack(user: User, id: number, trackId: string) {
    return this.mutate(id, async (event, save) => {
      if (!this.canView(event, user.id)) throw new ForbiddenException('Access denied');
      const [allowed, reason] = this.canVote(event, user.id);
      if (!allowed) throw new ForbiddenException(reason);

      const key = String(trackId);
      if (!event.songs.includes(key)) return { message: 'Already voted for this track or track not in event' };
      event.trackVotes[key] ??= [];
      if (event.trackVotes[key].includes(String(user.id))) {
        return { message: 'Already voted for this track or track not in event' };
      }
      event.trackVotes[key].push(String(user.id));
      await save(event);
      return { message: 'Vote added successfully' };
    });
  }

  async unvoteTrack(user: User, id: number, trackId: string) {
    return this.mutate(id, async (event, save) => {
      if (!this.canView(event, user.id)) throw new ForbiddenException('Access denied');
      const [allowed, reason] = this.canVote(event, user.id);
      if (!allowed) throw new ForbiddenException(reason);

      const key = String(trackId);
      const voters = event.trackVotes[key] ?? [];
      if (!voters.includes(String(user.id))) return { message: 'Vote not found' };

      event.trackVotes[key] = voters.filter((v) => v !== String(user.id));
      await save(event);
      return { message: 'Vote removed successfully' };
    });
  }

  async changeVoteLicense(user: User, id: number, voteLicense: string, start?: string, end?: string) {
    return this.mutate(id, async (event, save) => {
      if (!this.canEdit(event, user.id)) throw new ForbiddenException('Only organizer or managers can change the vote license');
      if (!VOTE_LICENSE_CHOICES.includes(voteLicense as any)) {
        throw new BadRequestException(`vote_license must be one of ${VOTE_LICENSE_CHOICES.join(', ')}`);
      }
      if (voteLicense === 'time_window') {
        if (!start || !end) {
          throw new BadRequestException('vote_window_start and vote_window_end (HH:MM) are required for the time_window license');
        }
        event.voteWindowStart = start;
        event.voteWindowEnd = end;
      }
      event.voteLicense = voteLicense as any;
      await save(event);
      return { message: `Vote license changed to ${voteLicense}` };
    });
  }

  async changeVisibility(user: User, id: number, isPublic: boolean) {
    return this.mutate(id, async (event, save) => {
      if (!this.canEdit(event, user.id)) throw new ForbiddenException('Only organizer or managers can change visibility');
      if (!isPublic && event.isPublic) assertPremium(event.organizer, 'Private events');
      event.isPublic = isPublic;
      await save(event);
      return { message: `Event visibility changed to ${isPublic ? 'public' : 'private'}` };
    });
  }

  async assignEditorRole(actor: User, id: number, userId: number) {
    return this.mutate(id, async (event, save) => {
      const target = await this.requireUser(userId);
      if (this.getUserRole(event, actor.id) !== 'owner') {
        throw new ForbiddenException('Only event owner can assign editor roles');
      }
      this.assignRole(event, userId, 'editor');
      await save(event);
      return { message: `User ${target.fullName} assigned as editor successfully` };
    });
  }

  async transferOwnership(actor: User, id: number, newOwnerId: number) {
    return this.mutate(id, async (event, save) => {
      const newOwner = await this.requireUser(newOwnerId);
      if (this.getUserRole(event, actor.id) !== 'owner') {
        throw new ForbiddenException('Only current owner can transfer ownership');
      }
      this.assignRole(event, newOwnerId, 'owner');
      this.assignRole(event, actor.id, 'editor');
      event.organizer = newOwner;
      event.organizerId = newOwner.id;
      await save(event);
      return { message: `Ownership transferred to ${newOwner.fullName} successfully` };
    });
  }

  async getUserRoles(actor: User | undefined, id: number) {
    const event = await this.load(id);
    if (!this.canView(event, actor?.id)) throw new ForbiddenException('Access denied');

    const usersByRole: Record<EventRole, unknown[]> = { owner: [], editor: [], listener: [] };
    for (const [userId, role] of Object.entries(event.userRoles)) {
      const user = await this.users.findById(Number(userId));
      if (user) usersByRole[role].push({ id: user.id, name: user.fullName, avatar: user.avatar, role });
    }
    return { userRoles: event.userRoles, usersByRole };
  }

  async removeUserRole(actor: User, id: number, userId: number) {
    return this.mutate(id, async (event, save) => {
      const target = await this.requireUser(userId);
      const currentRole = this.getUserRole(event, actor.id);
      if (currentRole !== 'owner' && actor.id !== userId) {
        throw new ForbiddenException('Only owner or the user themselves can remove from event');
      }
      if (this.getUserRole(event, userId) === 'owner' && actor.id !== userId) {
        throw new BadRequestException('Cannot remove the event owner');
      }
      if (!this.isAttendee(event, userId)) throw new BadRequestException('User is not part of this event');

      event.attendees = event.attendees.filter((a) => a.id !== userId);
      delete event.userRoles[String(userId)];
      await save(event);
      return { message: `User ${target.fullName} removed from event successfully` };
    });
  }

  private canInviteWithRole(event: Events, inviterId: number, role: string) {
    const isOrganizer = event.organizerId === inviterId;
    const isManager = this.isManager(event, inviterId);
    if (role === 'organizer') return isOrganizer;
    if (role === 'manager') return isOrganizer;
    if (role === 'attendee') return isOrganizer || isManager;
    return false;
  }

  async invite(actor: User, id: number, userId: number, role: 'organizer' | 'manager' | 'attendee') {
    return this.mutate(id, async (event, save) => {
      if (!this.canInviteWithRole(event, actor.id, role)) {
        throw new ForbiddenException(`You don't have permission to invite users as ${role}`);
      }
      const invitee = await this.requireUser(userId);
      const key = String(userId);
      if (this.isAttendee(event, userId)) throw new BadRequestException('User is already attending this event');
      if (event.pendingInvites.some((i) => i.userId === key)) {
        throw new BadRequestException('User already has a pending invite');
      }

      event.pendingInvites.push({ userId: key, role, invitedAt: new Date().toISOString() });
      await save(event);

      invitee.eventNotifications = {
        ...invitee.eventNotifications,
        [String(event.id)]: {
          eventId: event.id,
          eventTitle: event.title,
          inviterId: actor.id,
          inviterName: actor.fullName,
          invitedRole: role,
          type: 'event_invite',
          createdAt: new Date().toISOString(),
        },
      };
      await this.users.save(invitee);

      return { message: `Invitation sent successfully for ${role} role` };
    });
  }

  async acceptInvite(user: User, id: number) {
    return this.mutate(id, async (event, save) => {
      const invite = event.pendingInvites.find((i) => i.userId === String(user.id));
      if (!invite) throw new BadRequestException('No pending invite found');

      if (invite.role === 'organizer') {
        const previousOwner = event.organizer;
        event.organizer = user;
        event.organizerId = user.id;
        if (!event.managers.includes(String(previousOwner.id))) event.managers.push(String(previousOwner.id));
        if (!this.isAttendee(event, user.id)) event.attendees.push(user);
        this.assignRole(event, user.id, 'owner');
        this.assignRole(event, previousOwner.id, 'editor');
      } else if (invite.role === 'manager') {
        if (!event.managers.includes(String(user.id))) event.managers.push(String(user.id));
        if (!this.isAttendee(event, user.id)) event.attendees.push(user);
        this.assignRole(event, user.id, 'editor');
      } else {
        if (!this.isAttendee(event, user.id)) event.attendees.push(user);
        this.assignRole(event, user.id, 'listener');
      }

      event.pendingInvites = event.pendingInvites.filter((i) => i.userId !== String(user.id));
      await save(event);

      const notifications = { ...user.eventNotifications };
      delete notifications[String(event.id)];
      user.eventNotifications = notifications;
      await this.users.save(user);

      return { message: `Invitation accepted successfully. You are now a ${invite.role}` };
    });
  }

  async declineInvite(user: User, id: number) {
    return this.mutate(id, async (event, save) => {
      if (!event.pendingInvites.some((i) => i.userId === String(user.id))) {
        throw new BadRequestException('No pending invite found');
      }
      event.pendingInvites = event.pendingInvites.filter((i) => i.userId !== String(user.id));
      await save(event);

      const notifications = { ...user.eventNotifications };
      delete notifications[String(event.id)];
      user.eventNotifications = notifications;
      await this.users.save(user);

      return { message: 'Invitation declined' };
    });
  }

  async pendingInvitesFor(actor: User, id: number) {
    const event = await this.load(id);
    if (!this.hasPermission(event, actor.id, 'manage_users')) {
      throw new ForbiddenException('Only organizers and managers can view pending invites');
    }
    const invites = [];
    for (const invite of event.pendingInvites) {
      const user = await this.users.findById(Number(invite.userId));
      if (user) invites.push({ userId: invite.userId, name: user.fullName, email: user.email, role: invite.role, invitedAt: invite.invitedAt });
    }
    return { pendingInvites: invites, count: invites.length };
  }

  async myEvents(userId: number) {
    const events = await this.repo
      .createQueryBuilder('e')
      .leftJoinAndSelect('e.organizer', 'organizer')
      .leftJoinAndSelect('e.attendees', 'attendees')
      .where('e.organizerId = :userId', { userId })
      .orWhere('attendees.id = :userId', { userId })
      .orderBy('e.createdAt', 'DESC')
      .getMany();

    // managers is a JSON array, filtered in-memory since it's not relationally queryable.
    const managerEvents = await this.repo.find({ relations: ['organizer', 'attendees'] });
    const byManager = managerEvents.filter((e) => e.managers.includes(String(userId)));
    const all = [...events, ...byManager.filter((e) => !events.some((x) => x.id === e.id))];

    return {
      events: all.map((event) => ({
        id: event.id,
        title: event.title,
        description: event.description,
        location: event.location,
        startTime: event.eventStartTime,
        endTime: event.eventEndTime,
        isPublic: event.isPublic,
        organizer: event.organizer.fullName,
        createdAt: event.createdAt,
        updatedAt: event.updatedAt,
        attendeesCount: event.attendees.length,
        trackCount: event.songs.length,
        userRole: this.getUserRole(event, userId) ?? (event.organizerId === userId ? 'organizer' : this.isManager(event, userId) ? 'manager' : 'attendee'),
        canEdit: this.canEdit(event, userId),
      })),
      count: all.length,
    };
  }

  private async requireUser(id: number) {
    const user = await this.users.findById(id);
    if (!user) throw new NotFoundException('User not found');
    return user;
  }
}
