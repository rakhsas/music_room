import { Injectable, NestMiddleware } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { InjectRepository } from '@nestjs/typeorm';
import { Request, Response, NextFunction } from 'express';
import { Repository } from 'typeorm';
import { ActionLog } from './action-log.entity';

const SKIP_PREFIXES = ['/swagger', '/swagger-json'];

// Logs every mobile-app action reaching the backend (the subject requires it) - one
// place, covers every route including future ones, instead of per-route instrumentation.
@Injectable()
export class ActivityLogMiddleware implements NestMiddleware {
  constructor(
    @InjectRepository(ActionLog) private readonly repo: Repository<ActionLog>,
    private readonly jwt: JwtService,
  ) {}

  use(req: Request, res: Response, next: NextFunction) {
    res.on('finish', () => {
      if (SKIP_PREFIXES.some((prefix) => req.path.startsWith(prefix))) return;
      this.log(req, res).catch(() => {
        // Logging must never be the reason a real request fails.
      });
    });
    next();
  }

  private async log(req: Request, res: Response) {
    await this.repo.insert({
      userId: this.extractUserId(req),
      method: req.method,
      path: req.path.slice(0, 255),
      statusCode: res.statusCode,
      platform: (req.headers['x-platform'] as string) ?? '',
      deviceModel: (req.headers['x-device-model'] as string) ?? '',
      appVersion: (req.headers['x-app-version'] as string) ?? '',
      ipAddress: (req.headers['x-forwarded-for'] as string)?.split(',')[0]?.trim() ?? req.socket.remoteAddress ?? null,
    });
  }

  private extractUserId(req: Request): number | null {
    const header = req.headers.authorization;
    if (!header?.startsWith('Bearer ')) return null;
    try {
      return this.jwt.verify(header.slice(7)).sub ?? null;
    } catch {
      return null;
    }
  }
}
