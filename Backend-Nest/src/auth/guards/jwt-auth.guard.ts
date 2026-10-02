import { ExecutionContext, Injectable, UnauthorizedException } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { AuthGuard } from '@nestjs/passport';
import { IS_PUBLIC_KEY } from '../decorators/public.decorator';

// Registered as the global guard (see AppModule): every route needs a valid access
// token unless it is marked @Public().
@Injectable()
export class JwtAuthGuard extends AuthGuard('jwt') {
  constructor(private readonly reflector: Reflector) {
    super();
  }

  private isPublic(context: ExecutionContext): boolean {
    return (
      this.reflector.getAllAndOverride<boolean>(IS_PUBLIC_KEY, [
        context.getHandler(),
        context.getClass(),
      ]) ?? false
    );
  }

  canActivate(context: ExecutionContext) {
    // Always run the Passport strategy, even on @Public() routes - some of them (event/track
    // detail, attendee lists...) are "public" only in the sense of not requiring a token, but
    // still need to recognize a logged-in caller to decide ownership/attendance. Whether a
    // missing or invalid token is fatal is decided below in handleRequest.
    return super.canActivate(context);
  }

  handleRequest(err: any, user: any, info: any, context: ExecutionContext) {
    if (this.isPublic(context)) {
      // Optional auth: never reject the request - just pass through whatever caller (or none)
      // the token resolved to.
      return user || undefined;
    }
    if (err || !user) {
      throw err || new UnauthorizedException();
    }
    return user;
  }
}
