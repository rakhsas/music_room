import {
  Column,
  CreateDateColumn,
  Entity,
  ManyToOne,
  PrimaryGeneratedColumn,
  Unique,
} from 'typeorm';
import { User } from '../users/user.entity';
import { Device } from './device.entity';

@Entity('device_control_delegates')
@Unique(['device', 'delegate'])
export class ControlDelegate {
  @PrimaryGeneratedColumn()
  id: number;

  @ManyToOne(() => Device, (d) => d.delegates, { onDelete: 'CASCADE' })
  device: Device;

  @Column()
  deviceId: number;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  delegate: User;

  @Column()
  delegateId: number;

  @CreateDateColumn()
  grantedAt: Date;
}
