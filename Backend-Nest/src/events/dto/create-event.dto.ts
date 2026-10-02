import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsBoolean, IsIn, IsISO8601, IsOptional, IsString } from 'class-validator';
import { LOCATION_CHOICES } from '../event.entity';

export class CreateEventDto {
  @ApiProperty({ example: 'Friday Night Jam' })
  @IsString()
  title: string;

  @ApiPropertyOptional({ example: 'Come vote for the next tracks!' })
  @IsOptional()
  @IsString()
  description?: string;

  @ApiProperty({ enum: LOCATION_CHOICES, example: 'E1' })
  @IsIn(LOCATION_CHOICES)
  location: string;

  @ApiProperty({ example: '2026-09-20T20:00:00Z' })
  @IsISO8601()
  eventStartTime: string;

  @ApiPropertyOptional({ example: '2026-09-20T23:00:00Z' })
  @IsOptional()
  @IsISO8601()
  eventEndTime?: string;

  @ApiPropertyOptional({ example: true })
  @IsOptional()
  @IsBoolean()
  isPublic?: boolean;
}
