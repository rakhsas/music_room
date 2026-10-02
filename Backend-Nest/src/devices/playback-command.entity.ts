import {
  Column,
  CreateDateColumn,
  Entity,
  ManyToOne,
  PrimaryGeneratedColumn,
} from 'typeorm';
import { User } from '../users/user.entity';
import { Device } from './device.entity';

export type PlaybackCommandType = 'play' | 'pause' | 'skip';

@Entity('device_playback_commands')
export class PlaybackCommand {
  @PrimaryGeneratedColumn()
  id: number;

  @ManyToOne(() => Device, { onDelete: 'CASCADE' })
  device: Device;

  @Column()
  deviceId: number;

  @Column({ type: 'varchar', length: 10 })
  command: PlaybackCommandType;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  issuedBy: User;

  @Column()
  issuedById: number;

  @Column({ default: false })
  consumed: boolean;

  @CreateDateColumn()
  createdAt: Date;
}
