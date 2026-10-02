import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { UsersModule } from '../users/users.module';
import { Device } from './device.entity';
import { ControlDelegate } from './control-delegate.entity';
import { PlaybackCommand } from './playback-command.entity';
import { DevicesController } from './devices.controller';
import { DevicesService } from './devices.service';

@Module({
  imports: [TypeOrmModule.forFeature([Device, ControlDelegate, PlaybackCommand]), UsersModule],
  controllers: [DevicesController],
  providers: [DevicesService],
})
export class DevicesModule {}
