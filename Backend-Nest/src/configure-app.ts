import { INestApplication, ValidationPipe } from '@nestjs/common';

// Shared by main.ts and the e2e tests, so tests exercise the same pipes/prefix as production.
export function configureApp(app: INestApplication) {
  app.enableCors();
  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));
  // The mobile app is hardcoded to /api/<resource>/... URLs (see App/.../NetworkConfig.kt).
  app.setGlobalPrefix('api');
}
