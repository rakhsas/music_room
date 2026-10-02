import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

@Injectable()
export class AppService {
  constructor(private readonly config: ConfigService) {}

  getInfo() {
    return {
      service: 'musicroom-nest',
      environment: this.config.get<string>('NODE_ENV', 'development'),
    };
  }

  getHealth() {
    return { status: 'ok' };
  }
}
