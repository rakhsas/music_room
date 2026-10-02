import { ApiProperty } from '@nestjs/swagger';
import { Expose } from 'class-transformer';
import { IsString } from 'class-validator';

// snake_case on the wire - the Android logout already sends { refresh_token }.
export class RefreshTokenDto {
  @Expose({ name: 'refresh_token' })
  @ApiProperty({ name: 'refresh_token', description: 'Refresh token from login' })
  @IsString()
  refreshToken: string;
}
