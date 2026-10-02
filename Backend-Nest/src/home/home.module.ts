import { Module } from '@nestjs/common';
import { EventsModule } from '../events/events.module';
import { MusicModule } from '../music/music.module';
import { PlaylistsModule } from '../playlists/playlists.module';
import { HomeController } from './home.controller';
import { HomeService } from './home.service';

@Module({
  imports: [PlaylistsModule, MusicModule, EventsModule],
  controllers: [HomeController],
  providers: [HomeService],
})
export class HomeModule {}
