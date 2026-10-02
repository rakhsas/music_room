import {
  Column,
  CreateDateColumn,
  Entity,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from 'typeorm';

@Entity('users')
export class User {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ unique: true })
  email: string;

  @Column({ type: 'varchar', nullable: true })
  password: string | null;

  @Column()
  fullName: string;

  @Column({ unique: true })
  userName: string;
  

  @Column({ default: 'default_avatar.png' })
  avatar: string;

  @Column({ default: false })
  isVerified: boolean;

  @Column({ default: false })
  isActive: boolean;

  @Column({ type: 'text', default: '' })
  bio: string;

  @Column({ type: 'date', nullable: true })
  dateOfBirth: string | null;

  @Column({ default: '' })
  phoneNumber: string;

  @Column({ default: 'public' })
  profilePrivacy: string;

  @Column({ default: 'friends' })
  emailPrivacy: string;

  @Column({ default: 'private' })
  phonePrivacy: string;

  @Column({ type: 'varchar', nullable: true })
  googleId: string | null;

  @Column({ default: false })
  isPremium: boolean;

  @Column({ default: false })
  isSubscribed: boolean;

  @Column({ default: 'free' })
  subscriptionType: string;

  // Set by the (simulated) checkout - never the full card number.
  @Column({ type: 'timestamptz', nullable: true })
  premiumSince: Date | null;

  @Column({ type: 'varchar', nullable: true })
  cardBrand: string | null;

  @Column({ type: 'varchar', nullable: true })
  cardLast4: string | null;

  @Column({ type: 'jsonb', default: {} })
  musicPreferences: Record<string, unknown>;

  @Column({ type: 'jsonb', default: [] })
  likedArtists: string[];

  @Column({ type: 'jsonb', default: [] })
  likedAlbums: string[];

  @Column({ type: 'jsonb', default: [] })
  likedSongs: string[];

  @Column({ type: 'jsonb', default: [] })
  genres: string[];

  // Pending invites shown on the home screen, keyed by event/playlist id (as string).
  @Column({ type: 'jsonb', default: {} })
  eventNotifications: Record<string, unknown>;

  @Column({ type: 'jsonb', default: {} })
  playlistNotifications: Record<string, unknown>;

  @Column({ type: 'varchar', nullable: true })
  passwordResetOtp: string | null;

  @Column({ type: 'timestamptz', nullable: true })
  passwordResetOtpCreatedAt: Date | null;

  @Column({ default: false })
  passwordResetOtpVerified: boolean;

  // Embedded in every JWT as `ver`; bumping it on logout revokes all of this user's
  // outstanding tokens without a blacklist table.
  @Column({ default: 0 })
  tokenVersion: number;

  // One-directional: ids listed here may see this user's "friends"-level profile fields.
  @Column({ type: 'jsonb', default: [] })
  friendIds: number[];

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}
