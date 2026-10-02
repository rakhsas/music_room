import { ApiProperty } from '@nestjs/swagger';
import { IsArray, IsString } from 'class-validator';

export class ReorderTracksDto {
  @ApiProperty({ example: ['track2', 'track1', 'track3'] })
  @IsArray()
  @IsString({ each: true })
  trackOrder: string[];
}
