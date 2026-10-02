import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { User } from '../users/user.entity';
import { UsersService } from '../users/users.service';
import { Device } from './device.entity';
import { ControlDelegate } from './control-delegate.entity';
import { PlaybackCommand, PlaybackCommandType } from './playback-command.entity';

const VALID_COMMANDS: PlaybackCommandType[] = ['play', 'pause', 'skip'];

@Injectable()
export class DevicesService {
  constructor(
    @InjectRepository(Device) private readonly devices: Repository<Device>,
    @InjectRepository(ControlDelegate) private readonly delegates: Repository<ControlDelegate>,
    @InjectRepository(PlaybackCommand) private readonly commands: Repository<PlaybackCommand>,
    private readonly users: UsersService,
  ) {}

  async register(owner: User, deviceId: string, name: string, platform: string, appVersion: string) {
    let device = await this.devices.findOne({ where: { deviceId } });
    if (device) {
      device.owner = owner;
      device.ownerId = owner.id;
      device.name = name || device.name;
      device.platform = platform || device.platform;
      device.appVersion = appVersion || device.appVersion;
    } else {
      device = this.devices.create({ owner, ownerId: owner.id, deviceId, name, platform, appVersion });
    }
    await this.devices.save(device);
    return { message: 'Device registered', ...(await this.serialize(device)) };
  }

  async listMine(owner: User) {
    const devices = await this.devices.find({ where: { ownerId: owner.id } });
    return Promise.all(devices.map((d) => this.serialize(d)));
  }

  async listDelegatedToMe(user: User) {
    const rows = await this.delegates.find({
      where: { delegateId: user.id },
      relations: ['device', 'device.owner'],
    });
    return rows.map((d) => ({
      deviceId: d.device.deviceId,
      name: d.device.name,
      owner: { id: d.device.owner.id, name: d.device.owner.fullName },
    }));
  }

  async delegateControl(owner: User, deviceId: string, friendEmail: string) {
    const device = await this.devices.findOne({ where: { deviceId, ownerId: owner.id } });
    if (!device) throw new NotFoundException('Device not found for this account');

    const friend = await this.users.findByEmail(friendEmail);
    if (!friend || !friend.isActive) throw new NotFoundException('No user found with that email');
    if (friend.id === owner.id) throw new BadRequestException('You already control your own device');

    const existing = await this.delegates.findOne({ where: { deviceId: device.id, delegateId: friend.id } });
    if (existing) throw new BadRequestException(`${friend.fullName} already has control of this device`);

    await this.delegates.save(this.delegates.create({ device, deviceId: device.id, delegate: friend, delegateId: friend.id }));
    return { message: `${friend.fullName} can now control "${device.name || device.deviceId}"` };
  }

  async revokeControl(owner: User, deviceId: string, delegateUserId: number) {
    const device = await this.devices.findOne({ where: { deviceId, ownerId: owner.id } });
    if (!device) throw new NotFoundException('Device not found for this account');

    const result = await this.delegates.delete({ deviceId: device.id, delegateId: delegateUserId });
    if (!result.affected) throw new BadRequestException('That user does not have control of this device');
    return { message: 'Control revoked' };
  }

  async sendCommand(user: User, deviceId: string, command: string) {
    if (!VALID_COMMANDS.includes(command as PlaybackCommandType)) {
      throw new BadRequestException(`Invalid command. Must be one of ${VALID_COMMANDS.join(', ')}`);
    }
    const device = await this.devices.findOne({ where: { deviceId } });
    if (!device) throw new NotFoundException('Device not found');

    const canControl =
      device.ownerId === user.id ||
      (await this.delegates.exists({ where: { deviceId: device.id, delegateId: user.id } }));
    if (!canControl) throw new BadRequestException('You do not have control of this device');

    await this.commands.save(
      this.commands.create({ device, deviceId: device.id, command: command as PlaybackCommandType, issuedBy: user, issuedById: user.id }),
    );
    return { message: `${command} sent to ${device.name || device.deviceId}` };
  }

  async consumePendingCommands(owner: User, deviceId: string) {
    const device = await this.devices.findOne({ where: { deviceId, ownerId: owner.id } });
    if (!device) throw new NotFoundException('Device not found for this account');

    const pending = await this.commands.find({
      where: { deviceId: device.id, consumed: false },
      relations: ['issuedBy'],
      order: { createdAt: 'ASC' },
    });
    if (pending.length) {
      await this.commands.update(pending.map((c) => c.id), { consumed: true });
    }

    return {
      commands: pending.map((c) => ({
        command: c.command,
        issuedBy: { id: c.issuedBy.id, name: c.issuedBy.fullName },
        createdAt: c.createdAt,
      })),
    };
  }

  private async serialize(device: Device) {
    const delegates = await this.delegates.find({ where: { deviceId: device.id }, relations: ['delegate'] });
    return {
      deviceId: device.deviceId,
      name: device.name,
      platform: device.platform,
      appVersion: device.appVersion,
      lastSeenAt: device.lastSeenAt,
      delegates: delegates.map((d) => ({ id: d.delegate.id, name: d.delegate.fullName, email: d.delegate.email })),
    };
  }
}
