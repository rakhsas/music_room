import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { UsersModule } from '../users/users.module';
import { MusicModule } from '../music/music.module';
import { AuthModule } from '../auth/auth.module';
import { Playlist } from './playlist.entity';
import { PlaylistsController } from './playlists.controller';
import { PlaylistsService } from './playlists.service';
import { PlaylistsGateway } from './playlists.gateway';

@Module({
  imports: [TypeOrmModule.forFeature([Playlist]), UsersModule, MusicModule, AuthModule],
  controllers: [PlaylistsController],
  providers: [PlaylistsService, PlaylistsGateway],
  exports: [PlaylistsService],
})
export class PlaylistsModule {}
