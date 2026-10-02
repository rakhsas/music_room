import { Controller, Get, Param, Query } from '@nestjs/common';
import { ApiBearerAuth, ApiQuery, ApiTags } from '@nestjs/swagger';
import { UsersService } from '../users/users.service';
import { JamendoService } from './jamendo.service';
import { ApiDoc, JAMENDO_ARTIST, JAMENDO_TRACK, jamendoList } from '../utils/api-docs';

const TRACKS = jamendoList(JAMENDO_TRACK);
const ARTISTS = jamendoList(JAMENDO_ARTIST);
const RAW = 'Raw Jamendo v3.0 response: `headers` + `results` array.';
const LimitQuery = (dflt: number) => ApiQuery({ name: 'limit', required: false, type: Number, example: dflt });

@ApiTags('music')
@ApiBearerAuth()
@Controller('music')
export class MusicController {
  constructor(
    private readonly jamendo: JamendoService,
    private readonly users: UsersService,
  ) {}

  @Get('songs')
  @LimitQuery(20)
  @ApiDoc('Random songs', TRACKS, { description: RAW })
  songList(@Query('limit') limit?: string) {
    return this.jamendo.randomSongs(limit ? Number(limit) : 20);
  }

  @Get('related')
  @ApiQuery({ name: 'user_id', required: false, type: Number, description: "Use this user's genres (default: rock)" })
  @LimitQuery(10)
  @ApiDoc('Songs matching a user\'s favourite genres', TRACKS, { description: RAW })
  async related(@Query('user_id') userId?: string, @Query('limit') limit?: string) {
    const user = userId ? await this.users.findById(Number(userId)) : undefined;
    return this.jamendo.relatedForGenres(user?.genres ?? [], limit ? Number(limit) : 10);
  }

  @Get('songs/:trackId')
  @ApiDoc('Song detail', TRACKS, { description: `${RAW} \`results\` holds the one track (empty if the id is unknown).` })
  songDetail(@Param('trackId') trackId: string) {
    return this.jamendo.trackDetail(trackId);
  }

  @Get('random-songs')
  @LimitQuery(10)
  @ApiDoc('Random songs', TRACKS, { description: RAW })
  randomSongs(@Query('limit') limit?: string) {
    return this.jamendo.randomSongs(limit ? Number(limit) : 10);
  }

  @Get('tracks/search-name/:query')
  @ApiDoc('Search tracks by name (max 10)', TRACKS, { description: RAW })
  trackSearch(@Param('query') query: string) {
    return this.jamendo.trackSearch(query);
  }

  @Get('artists')
  @ApiDoc('10 artists', ARTISTS, { description: RAW })
  artistList() {
    return this.jamendo.popularArtists(10);
  }

  @Get('artists/popular')
  @LimitQuery(10)
  @ApiDoc('Popular artists', ARTISTS, { description: RAW })
  popularArtists(@Query('limit') limit?: string) {
    return this.jamendo.popularArtists(limit ? Number(limit) : 10);
  }

  @Get('artists/search/:query')
  @ApiDoc('Search artists by name', ARTISTS, { description: RAW })
  artistSearch(@Param('query') query: string) {
    return this.jamendo.artistSearch(query);
  }

  // Registered after the static 'popular'/'search/:query' routes above so it doesn't shadow them.
  @Get('artists/:id')
  @ApiDoc('Artist detail + up to 50 of their tracks', { artist: JAMENDO_ARTIST, tracks: [JAMENDO_TRACK] }, {
    description: '`artist` is null if the id is unknown.',
  })
  async artistDetail(@Param('id') id: string) {
    const [artist, tracks] = await Promise.all([this.jamendo.artistById(id), this.jamendo.tracksByArtistId(id)]);
    return { artist: artist.results?.[0] ?? null, tracks: tracks.results ?? [] };
  }
}
