import { Injectable, Logger, OnModuleDestroy } from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { ConfigService } from '@nestjs/config';
import { firstValueFrom } from 'rxjs';
import Redis from 'ioredis';

const CACHE_TTL_SECONDS = 3600;

@Injectable()
export class JamendoService implements OnModuleDestroy {
  private readonly baseUrl = 'https://api.jamendo.com/v3.0';
  private readonly logger = new Logger(JamendoService.name);
  private readonly redis: Redis;

  constructor(
    private readonly http: HttpService,
    private readonly config: ConfigService,
  ) {
    // enableOfflineQueue:false - if Redis is down, cache calls fail fast (-> live Jamendo)
    // instead of queueing through ~20 reconnect attempts and stalling every request.
    this.redis = new Redis(this.config.getOrThrow<string>('REDIS_URL'), { enableOfflineQueue: false });
    this.redis.on('error', (err) => this.logger.warn(`Redis unavailable, falling back to live Jamendo calls: ${err.message}`));
  }

  onModuleDestroy() {
    this.redis.disconnect();
  }

  private get clientId() {
    return this.config.get<string>('JAMENDO_CLIENT_ID');
  }

  // Jamendo's catalogue barely changes minute to minute, so responses are cached
  // in Redis for CACHE_TTL_SECONDS - callers like events/playlists "get tracks"
  // otherwise hit the live Jamendo API on every single request. Skipped for
  // randomSongs(), where a cache hit would defeat the point of "random".
  private async get(path: string, params: Record<string, unknown>, cache = true) {
    const cacheKey = cache ? `jamendo:${path}:${JSON.stringify(params)}` : null;

    if (cacheKey) {
      const cached = await this.redis.get(cacheKey).catch(() => null);
      if (cached) return JSON.parse(cached);
    }

    // Jamendo intermittently answers "success" with zero results for queries that do
    // match (measured ~30-50% of calls) - retry, and never cache an empty/failed
    // response, or that blip gets pinned in Redis for the full TTL.
    let data: any;
    for (let attempt = 0; attempt < 3; attempt++) {
      ({ data } = await firstValueFrom(
        this.http.get(`${this.baseUrl}${path}`, {
          params: { client_id: this.clientId, format: 'json', ...params },
        }),
      ));
      if (data?.results?.length) break;
    }

    if (cacheKey && data?.headers?.status === 'success' && data.results?.length) {
      this.redis.set(cacheKey, JSON.stringify(data), 'EX', CACHE_TTL_SECONDS).catch(() => {});
    }
    return data;
  }

  randomSongs(limit = 10) {
    return this.get('/tracks', { limit }, false);
  }

  trackDetail(trackId: string | number) {
    return this.get('/tracks', { id: trackId });
  }

  trackSearch(query: string) {
    return this.get('/tracks', { namesearch: query, limit: 10 });
  }

  relatedForGenres(genres: string[], limit = 10) {
    return this.get('/tracks', { tags: genres.length ? genres : 'rock', limit });
  }

  tracksByIds(trackIds: string[]) {
    if (!trackIds.length) return Promise.resolve({ results: [] });
    // include=musicinfo makes Jamendo's `id` filter silently match nothing (verified directly
    // against their API) - dropping it is what makes lookups by id actually return the tracks.
    return this.get('/tracks', { id: trackIds.join('+') });
  }

  popularArtists(limit = 10) {
    return this.get('/artists', { limit });
  }

  artistSearch(query: string) {
    return this.get('/artists', { namesearch: query });
  }

  artistById(id: string) {
    return this.get('/artists', { id });
  }

  tracksByArtistId(artistId: string, limit = 50) {
    return this.get('/tracks', { artist_id: artistId, limit });
  }
}
