import { Controller, Delete, Get, Param, Post, Body } from '@nestjs/common';
import { ApiBearerAuth, ApiParam, ApiTags } from '@nestjs/swagger';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { User } from '../users/user.entity';
import { DevicesService } from './devices.service';
import { RegisterDeviceDto } from './dto/register-device.dto';
import { DelegateControlDto } from './dto/delegate-control.dto';
import { ApiDoc, msg } from '../utils/api-docs';

const DEVICE = {
  deviceId: 'device-1234',
  name: 'My Phone',
  platform: 'Android',
  appVersion: '1.0.0',
  lastSeenAt: '2026-09-20T20:00:00.000Z',
  delegates: [{ id: 2, name: 'Jane Doe', email: 'jane@example.com' }],
};
const NOT_MINE = { 404: 'Device not found for this account' };

@ApiTags('devices')
@ApiBearerAuth()
@Controller('devices')
export class DevicesController {
  constructor(private readonly devices: DevicesService) {}

  @Post('register')
  @ApiDoc('Register (or re-claim) this device', { message: 'Device registered', ...DEVICE }, { status: 201 })
  register(@CurrentUser() user: User, @Body() dto: RegisterDeviceDto) {
    return this.devices.register(user, dto.deviceId, dto.name ?? '', dto.platform ?? '', dto.appVersion ?? '');
  }

  @Get('my-devices')
  @ApiDoc('List my devices', [DEVICE])
  myDevices(@CurrentUser() user: User) {
    return this.devices.listMine(user);
  }

  @Get('delegated-to-me')
  @ApiDoc('Devices other users let me control', [{ deviceId: 'device-5678', name: "Jane's Phone", owner: { id: 2, name: 'Jane Doe' } }])
  delegatedToMe(@CurrentUser() user: User) {
    return this.devices.listDelegatedToMe(user);
  }

  @Post(':deviceId/delegate')
  @ApiDoc('Let a friend control my device', msg('Jane Doe can now control "My Phone"'), {
    status: 201,
    errors: {
      400: 'You already control your own device | Jane Doe already has control of this device',
      404: 'Device not found for this account | No user found with that email',
    },
  })
  delegate(@CurrentUser() user: User, @Param('deviceId') deviceId: string, @Body() dto: DelegateControlDto) {
    return this.devices.delegateControl(user, deviceId, dto.email);
  }

  @Delete(':deviceId/delegate/:userId')
  @ApiDoc('Revoke a friend\'s control', msg('Control revoked'), {
    errors: { 400: 'That user does not have control of this device', ...NOT_MINE },
  })
  revoke(@CurrentUser() user: User, @Param('deviceId') deviceId: string, @Param('userId') userId: string) {
    return this.devices.revokeControl(user, deviceId, Number(userId));
  }

  @Post(':deviceId/control/:action')
  @ApiParam({ name: 'action', enum: ['play', 'pause', 'skip'] })
  @ApiDoc('Send a playback command to a device', msg('pause sent to My Phone'), {
    status: 201,
    description: 'Allowed for the owner and delegates. The device picks it up via pending-commands.',
    errors: { 400: 'Invalid command. Must be one of play, pause, skip | You do not have control of this device', 404: 'Device not found' },
  })
  sendCommand(@CurrentUser() user: User, @Param('deviceId') deviceId: string, @Param('action') action: string) {
    return this.devices.sendCommand(user, deviceId, action);
  }

  @Get(':deviceId/pending-commands')
  @ApiDoc(
    'Fetch (and consume) queued commands for my device',
    { commands: [{ command: 'pause', issuedBy: { id: 2, name: 'Jane Doe' }, createdAt: '2026-09-20T20:01:00.000Z' }] },
    { description: 'Oldest first. Returned commands are marked consumed and won\'t be returned again.', errors: NOT_MINE },
  )
  pendingCommands(@CurrentUser() user: User, @Param('deviceId') deviceId: string) {
    return this.devices.consumePendingCommands(user, deviceId);
  }
}
