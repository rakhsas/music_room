import { ApiProperty } from '@nestjs/swagger';
import { Expose } from 'class-transformer';
import { IsIn, IsString } from 'class-validator';

export class SocialLoginDto {
  @ApiProperty({ enum: ['google'], example: 'google' })
  @IsIn(['google'])
  provider: 'google';

  // Android sends snake_case (see App/.../AuthApiService.kt's socialLogin()),
  // while this DTO's property is
  // camelCase everywhere else - @Expose remaps the incoming JSON key so
  // ValidationPipe's whitelist:true doesn't silently drop it as unrecognized.
  @Expose({ name: 'access_token' })
  @ApiProperty({ name: 'access_token', description: 'Access token (or ID token for Google)', example: 'sample-access-token' })
  @IsString()
  accessToken: string;
}
