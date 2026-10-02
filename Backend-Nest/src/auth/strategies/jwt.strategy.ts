import { Injectable, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';
import { UsersService } from '../../users/users.service';

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy) {
  constructor(config: ConfigService, private readonly users: UsersService) {
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      secretOrKey: config.getOrThrow<string>('SECRET_KEY'),
    });
  }

  async validate(payload: { sub: number; type?: string; ver?: number }) {
    // Single-purpose tokens (e.g. email verification) are signed with the same
    // secret but carry no type:'access' - reject them here so they can't be
    // replayed as a bearer token against the rest of the API.
    if (payload.type !== 'access') throw new UnauthorizedException();

    const user = await this.users.findById(payload.sub);
    // Revoked by logout (tokenVersion bumped after this token was issued).
    if (!user || (payload.ver ?? 0) !== user.tokenVersion) throw new UnauthorizedException();
    return user;
  }
}
