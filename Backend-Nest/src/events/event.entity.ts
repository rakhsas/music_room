import {
  Column,
  CreateDateColumn,
  Entity,
  JoinTable,
  ManyToMany,
  ManyToOne,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from 'typeorm';
import { User } from '../users/user.entity';

export const LOCATION_CHOICES = [
  'E1', 'E2', 'P1', 'P2', 'C3', 'C4', 'Agora', 'E3',
  'C3-Room', 'C3-Relax', 'C4-rooms', 'Elevator-room',
] as const;

export const VOTE_LICENSE_CHOICES = ['everyone', 'invited_only', 'time_window'] as const;
export type VoteLicense = (typeof VOTE_LICENSE_CHOICES)[number];

export type EventRole = 'owner' | 'editor' | 'listener';

export interface PendingEventInvite {
  userId: string;
  role: 'organizer' | 'manager' | 'attendee';
  invitedAt: string;
}

@Entity('events')
export class Events {
  @PrimaryGeneratedColumn()
  id: number;

  @Column()
  title: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  organizer: User;

  @Column()
  organizerId: number;

  @ManyToMany(() => User)
  @JoinTable({ name: 'event_attendees' })
  attendees: User[];

  @Column({ type: 'text', nullable: true })
  description: string | null;

  @Column()
  location: string;

  @Column({ type: 'varchar', nullable: true })
  imageUrl: string | null;

  @Column({ type: 'jsonb', default: [] })
  songs: string[];

  // User ids (as strings) with the "manager" relationship.
  @Column({ type: 'jsonb', default: [] })
  managers: string[];

  @Column({ type: 'jsonb', default: {} })
  trackVotes: Record<string, string[]>;

  @Column({ type: 'jsonb', default: {} })
  userRoles: Record<string, EventRole>;

  @Column({ type: 'jsonb', default: [] })
  pendingInvites: PendingEventInvite[];

  @Column({ default: true })
  isPublic: boolean;

  @Column({ type: 'varchar', default: 'everyone' })
  voteLicense: VoteLicense;

  @Column({ type: 'varchar', nullable: true })
  voteWindowStart: string | null;

  @Column({ type: 'varchar', nullable: true })
  voteWindowEnd: string | null;

  @Column({ type: 'timestamptz' })
  eventStartTime: Date;

  @Column({ type: 'timestamptz', nullable: true })
  eventEndTime: Date | null;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}
