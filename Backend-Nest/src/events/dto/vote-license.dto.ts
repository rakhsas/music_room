import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsIn, IsOptional, IsString } from 'class-validator';
import { VOTE_LICENSE_CHOICES } from '../event.entity';

export class ChangeVoteLicenseDto {
  @ApiProperty({ enum: VOTE_LICENSE_CHOICES, example: 'everyone' })
  @IsIn(VOTE_LICENSE_CHOICES)
  voteLicense: string;

  @ApiPropertyOptional({ example: '16:00', description: 'HH:MM, required for time_window' })
  @IsOptional()
  @IsString()
  voteWindowStart?: string;

  @ApiPropertyOptional({ example: '18:00', description: 'HH:MM, required for time_window' })
  @IsOptional()
  @IsString()
  voteWindowEnd?: string;
}
