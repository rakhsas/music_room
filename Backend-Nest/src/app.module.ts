import { MiddlewareConsumer, Module, NestModule } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ThrottlerModule } from '@nestjs/throttler';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { ActivitylogModule } from './activitylog/activitylog.module';
import { ActivityLogMiddleware } from './activitylog/activity-log.middleware';
import { AuthModule } from './auth/auth.module';
import { JwtAuthGuard } from './auth/guards/jwt-auth.guard';
import { DevicesModule } from './devices/devices.module';
import { EventsModule } from './events/events.module';
import { HomeModule } from './home/home.module';
import { MusicModule } from './music/music.module';
import { PlaylistsModule } from './playlists/playlists.module';
import { UsersModule } from './users/users.module';

@Module({
  imports: [
    // One .env for the whole repo, at the root. On the host (`npm run start:dev` from
    // Backend-Nest/) it is read from ../.env; in Docker the file isn't in the image and
    // docker-compose injects it as real env vars instead. Real env vars always win.
    ConfigModule.forRoot({ isGlobal: true, envFilePath: '../.env' }),
    // Registers the throttler storage/config; the guard itself is applied only
    // on AuthController (@UseGuards(ThrottlerGuard) there), not globally - a
    // global per-IP guard would throttle ordinary API traffic from any shared
    // IP (NAT, mobile carrier, this load test) along with real brute force.
    ThrottlerModule.forRoot([{ ttl: 60_000, limit: 60 }]),
    TypeOrmModule.forRootAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      // No migrations: synchronize:true creates/updates the tables from the
      // entities on startup.
      useFactory: (config: ConfigService) => ({
        type: 'postgres',
        url: config.getOrThrow<string>('DATABASE_URL'),
        autoLoadEntities: true,
        synchronize: true,
      }),
    }),
    // AuthModule before UsersModule: both controllers are mounted at 'users',
    // and UsersController's GET /users/:userName would otherwise register
    // first and shadow AuthController's static GET /users/profile and
    // /users/verify-email routes (Nest/Express match routes in registration order).
    AuthModule,
    UsersModule,
    MusicModule,
    EventsModule,
    PlaylistsModule,
    DevicesModule,
    HomeModule,
    ActivitylogModule,
  ],
  controllers: [AppController],
  providers: [AppService, { provide: APP_GUARD, useClass: JwtAuthGuard }],
})
export class AppModule implements NestModule {
  configure(consumer: MiddlewareConsumer) {
    consumer.apply(ActivityLogMiddleware).forRoutes('*');
  }
}
