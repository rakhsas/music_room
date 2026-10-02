import { ApiProperty } from '@nestjs/swagger';
import { IsEmail } from 'class-validator';

export class DelegateControlDto {
  @ApiProperty({ description: "Friend's account email", example: 'friend@example.com' })
  @IsEmail()
  email: string;
}
