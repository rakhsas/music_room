import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { JwtModule } from '@nestjs/jwt';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { ActionLog } from './action-log.entity';
import { ActivityLogMiddleware } from './activity-log.middleware';

@Module({
  imports: [
    TypeOrmModule.forFeature([ActionLog]),
    JwtModule.registerAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({ secret: config.getOrThrow<string>('SECRET_KEY') }),
    }),
  ],
  providers: [ActivityLogMiddleware],
  // AppModule.configure() instantiates this middleware in its own injector scope,
  // so both the repository and JwtModule it depends on must be re-exported here,
  // not just the middleware class itself.
  exports: [ActivityLogMiddleware, TypeOrmModule, JwtModule],
})
export class ActivitylogModule {}
