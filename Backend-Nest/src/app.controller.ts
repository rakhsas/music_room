import { Controller, Get } from '@nestjs/common';
import { ApiTags } from '@nestjs/swagger';
import { Public } from './auth/decorators/public.decorator';
import { AppService } from './app.service';
import { ApiDoc } from './utils/api-docs';

@ApiTags('app')
@Controller()
export class AppController {
  constructor(private readonly appService: AppService) {}

  @Public()
  @Get()
  @ApiDoc('Service info', { service: 'musicroom-nest', environment: 'development' })
  getInfo() {
    return this.appService.getInfo();
  }

  @Public()
  @Get('health')
  @ApiDoc('Health check', { status: 'ok' })
  getHealth() {
    return this.appService.getHealth();
  }
}
