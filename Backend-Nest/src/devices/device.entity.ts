import {
  Column,
  CreateDateColumn,
  Entity,
  ManyToOne,
  OneToMany,
  PrimaryGeneratedColumn,
  UpdateDateColumn,
} from 'typeorm';
import { User } from '../users/user.entity';
import { ControlDelegate } from './control-delegate.entity';

@Entity('devices')
export class Device {
  @PrimaryGeneratedColumn()
  id: number;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  owner: User;

  @Column()
  ownerId: number;

  @Column({ unique: true })
  deviceId: string;

  @Column({ default: '' })
  name: string;

  @Column({ default: '' })
  platform: string;

  @Column({ default: '' })
  appVersion: string;

  @OneToMany(() => ControlDelegate, (d) => d.device)
  delegates: ControlDelegate[];

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  lastSeenAt: Date;
}
