import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsIn, IsInt, IsOptional } from 'class-validator';

export class InviteUserDto {
  @ApiProperty({ example: 1 })
  @IsInt()
  userId: number;

  @ApiPropertyOptional({ enum: ['organizer', 'manager', 'attendee'], default: 'attendee' })
  @IsOptional()
  @IsIn(['organizer', 'manager', 'attendee'])
  role?: 'organizer' | 'manager' | 'attendee';
}
