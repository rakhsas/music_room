import { ApiProperty } from '@nestjs/swagger';
import { Expose } from 'class-transformer';
import { IsEmail, IsString, MinLength } from 'class-validator';

// Field names on the wire are snake_case (full_name, username) - that's what the
// Android client sends. @Expose remaps them since this DTO's properties are camelCase
// everywhere else and ValidationPipe's whitelist:true would otherwise silently drop
// the snake_case keys.
export class RegisterDto {
  @Expose({ name: 'full_name' })
  @ApiProperty({ name: 'full_name', example: 'John Doe' })
  @IsString()
  fullName: string;

  @Expose({ name: 'username' })
  @ApiProperty({ name: 'username', example: 'johndoe' })
  @IsString()
  userName: string;

  @ApiProperty({ example: 'john@example.com' })
  @IsEmail()
  email: string;

  @ApiProperty({ example: 'Passw0rd!', minLength: 8 })
  @IsString()
  @MinLength(8)
  password: string;
}
