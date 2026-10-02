import { ApiPropertyOptional } from '@nestjs/swagger';
import { Expose, Transform } from 'class-transformer';
import { IsArray, IsDateString, IsIn, IsObject, IsOptional, IsString } from 'class-validator';

const PRIVACY = ['public', 'friends', 'private'];

// Field names on the wire are snake_case - that's what the Android client sends.
// @Expose remaps them since this DTO's properties are camelCase everywhere else and
// ValidationPipe's whitelist:true would otherwise silently drop the snake_case keys.
export class UpdateProfileDto {
  @Expose({ name: 'full_name' })
  @ApiPropertyOptional({ name: 'full_name', example: 'John Doe' })
  @IsOptional()
  @IsString()
  fullName?: string;

  @Expose({ name: 'userName' })
  @ApiPropertyOptional({ name: 'userName', example: 'johndoe' })
  @IsOptional()
  @IsString()
  userName?: string;

  @ApiPropertyOptional({ example: 'Music lover and playlist curator.' })
  @IsOptional()
  @IsString()
  bio?: string;

  @Expose({ name: 'date_of_birth' })
  @ApiPropertyOptional({ name: 'date_of_birth', example: '1995-06-15' })
  @IsOptional()
  // The web form sends "" when the date is cleared - Postgres rejects "" for a date column.
  @Transform(({ value }) => (value === '' ? null : value))
  @IsDateString()
  dateOfBirth?: string;

  @Expose({ name: 'phone_number' })
  @ApiPropertyOptional({ name: 'phone_number', example: '+33612345678' })
  @IsOptional()
  @IsString()
  phoneNumber?: string;

  @Expose({ name: 'profile_privacy' })
  @ApiPropertyOptional({ name: 'profile_privacy', example: 'public' })
  @IsOptional()
  @IsIn(PRIVACY)
  profilePrivacy?: string;

  @Expose({ name: 'email_privacy' })
  @ApiPropertyOptional({ name: 'email_privacy', example: 'private' })
  @IsOptional()
  @IsIn(PRIVACY)
  emailPrivacy?: string;

  @Expose({ name: 'phone_privacy' })
  @ApiPropertyOptional({ name: 'phone_privacy', example: 'private' })
  @IsOptional()
  @IsIn(PRIVACY)
  phonePrivacy?: string;

  @Expose({ name: 'music_preferences' })
  @ApiPropertyOptional({ name: 'music_preferences', example: { favorite_genre: 'rock' } })
  @IsOptional()
  @IsObject()
  musicPreferences?: Record<string, unknown>;

  @Expose({ name: 'liked_artists' })
  @ApiPropertyOptional({ name: 'liked_artists', example: ['Daft Punk'] })
  @IsOptional()
  @IsArray()
  likedArtists?: string[];

  @Expose({ name: 'liked_albums' })
  @ApiPropertyOptional({ name: 'liked_albums', example: ['Discovery'] })
  @IsOptional()
  @IsArray()
  likedAlbums?: string[];

  @Expose({ name: 'liked_songs' })
  @ApiPropertyOptional({ name: 'liked_songs', example: ['One More Time'] })
  @IsOptional()
  @IsArray()
  likedSongs?: string[];

  @ApiPropertyOptional({ example: ['rock', 'electronic'] })
  @IsOptional()
  @IsArray()
  genres?: string[];
}
