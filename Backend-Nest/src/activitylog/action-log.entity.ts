import { Column, CreateDateColumn, Entity, PrimaryGeneratedColumn } from 'typeorm';

@Entity('action_logs')
export class ActionLog {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ type: 'int', nullable: true })
  userId: number | null;

  @Column({ length: 10 })
  method: string;

  @Column({ length: 255 })
  path: string;

  @Column()
  statusCode: number;

  @Column({ length: 50, default: '' })
  platform: string;

  @Column({ length: 100, default: '' })
  deviceModel: string;

  @Column({ length: 50, default: '' })
  appVersion: string;

  @Column({ type: 'varchar', nullable: true })
  ipAddress: string | null;

  @CreateDateColumn()
  createdAt: Date;
}
