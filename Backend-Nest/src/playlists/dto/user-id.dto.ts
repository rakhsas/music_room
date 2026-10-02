import { ApiProperty } from '@nestjs/swagger';
import { IsInt } from 'class-validator';

export class UserIdDto {
  @ApiProperty({ example: 1 })
  @IsInt()
  userId: number;
}
