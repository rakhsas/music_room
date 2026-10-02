import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, IsString } from 'class-validator';

export class RegisterDeviceDto {
  @ApiProperty({ description: 'Stable client-generated device identifier', example: 'device-1234' })
  @IsString()
  deviceId: string;

  @ApiPropertyOptional({ example: 'My Phone' })
  @IsOptional()
  @IsString()
  name?: string;

  @ApiPropertyOptional({ example: 'Android' })
  @IsOptional()
  @IsString()
  platform?: string;

  @ApiPropertyOptional({ example: '1.0.0' })
  @IsOptional()
  @IsString()
  appVersion?: string;
}
