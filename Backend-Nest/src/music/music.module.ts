import { Module } from '@nestjs/common';
import { HttpModule } from '@nestjs/axios';
import { UsersModule } from '../users/users.module';
import { JamendoService } from './jamendo.service';
import { MusicController } from './music.controller';

@Module({
  imports: [HttpModule, UsersModule],
  controllers: [MusicController],
  providers: [JamendoService],
  exports: [JamendoService],
})
export class MusicModule {}
