import { NestFactory } from '@nestjs/core';
import { ConfigService } from '@nestjs/config';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import { AppModule } from './app.module';
import { configureApp } from './configure-app';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  configureApp(app);

  const document = SwaggerModule.createDocument(
    app,
    new DocumentBuilder()
      .setTitle('MusicRoom API')
      .setDescription(
        'Auth, events, playlists, music, devices and home endpoints.\n\n' +
          '**Auth:** log in via `POST /api/users/login`, click *Authorize* and paste `tokens.access`. ' +
          'Routes with a lock return `401 Unauthorized` without a valid token.\n\n' +
          '**Errors** always look like `{ "statusCode": 403, "message": "...", "error": "Forbidden" }`. ' +
          'Body validation errors are `400` with `message` as an array of strings.',
      )
      .setVersion('0.2.0')
      .addBearerAuth()
      .build(),
  );
  SwaggerModule.setup('swagger', app, document);

  const config = app.get(ConfigService);
  const port = config.get<number>('PORT', 8000);

  await app.listen(port, '0.0.0.0');
}
bootstrap();
