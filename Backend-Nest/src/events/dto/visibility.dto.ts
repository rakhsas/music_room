import { ApiProperty } from '@nestjs/swagger';
import { IsBoolean } from 'class-validator';

export class VisibilityDto {
  @ApiProperty({ example: true, description: 'true for public, false for private' })
  @IsBoolean()
  isPublic: boolean;
}
