import {
  Column,
  CreateDateColumn,
  Entity,
  JoinTable,
  ManyToMany,
  ManyToOne,
  PrimaryGeneratedColumn,
} from 'typeorm';
import { User } from '../users/user.entity';

@Entity('playlists')
export class Playlist {
  @PrimaryGeneratedColumn()
  id: number;

  @Column()
  name: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  owner: User;

  @Column()
  ownerId: number;

  @Column({ type: 'jsonb', default: [] })
  tracks: string[];

  @ManyToMany(() => User)
  @JoinTable({ name: 'playlist_collaborators' })
  collaborators: User[];

  @ManyToMany(() => User)
  @JoinTable({ name: 'playlist_followers' })
  followers: User[];

  // Extra per-user edit grants beyond owner/collaborator.
  @Column({ type: 'jsonb', default: [] })
  couldEdit: string[];

  @Column({ type: 'jsonb', default: [] })
  pendingInvites: string[];

  @Column({ default: true })
  isPublic: boolean;

  @CreateDateColumn()
  date: Date;
}
