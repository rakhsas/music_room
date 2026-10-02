import { Body, Controller, Delete, Get, Param, ParseIntPipe, Post } from '@nestjs/common';
import { ApiBearerAuth, ApiProperty, ApiTags } from '@nestjs/swagger';
import { IsString } from 'class-validator';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { User } from './user.entity';
import { UsersService } from './users.service';
import { ApiDoc, msg } from '../utils/api-docs';

class AddFriendDto {
  @ApiProperty({ example: 'janedoe' })
  @IsString()
  userName: string;
}

@ApiTags('users')
@ApiBearerAuth()
@Controller('users')
export class UsersController {
  constructor(private readonly users: UsersService) {}

  @Get()
  @ApiDoc(
    'List all active users except me',
    [{ id: 2, fullName: 'Jane Doe', userName: 'janedoe', email: null, avatar: 'default_avatar.png' }],
    { description: '`email` is null unless that user\'s emailPrivacy lets you see it.' },
  )
  findAll(@CurrentUser() user: User) {
    return this.users.findAllExcept(user.id);
  }

  // Static "friends" routes must be declared before ":userName", which would swallow them.
  @Get('friends/list')
  @ApiDoc('List my friends', [{ id: 2, fullName: 'Jane Doe', userName: 'janedoe', avatar: 'default_avatar.png' }])
  friends(@CurrentUser() user: User) {
    return this.users.friends(user);
  }

  @Post('friends/add')
  @ApiDoc('Add a friend (lets them see my "friends"-level info)', msg('Jane Doe can now see your friends-only info'), {
    status: 201,
    description: 'One-directional: it only grants THEM access to MY friends-only fields.',
    errors: { 404: 'User not found' },
  })
  addFriend(@CurrentUser() user: User, @Body() dto: AddFriendDto) {
    return this.users.addFriend(user, dto.userName);
  }

  @Delete('friends/:userId')
  @ApiDoc('Remove a friend', msg('Friend removed'))
  removeFriend(@CurrentUser() user: User, @Param('userId', ParseIntPipe) userId: number) {
    return this.users.removeFriend(user, userId);
  }

  @Get(':userName')
  @ApiDoc(
    'Get a user\'s public profile by username',
    {
      id: 2,
      fullName: 'Jane Doe',
      userName: 'janedoe',
      avatar: 'default_avatar.png',
      email: 'jane@example.com',
      phoneNumber: null,
      bio: 'Techno addict.',
      dateOfBirth: '1996-02-10',
      musicPreferences: {},
      likedArtists: [],
      likedAlbums: [],
      likedSongs: [],
      genres: ['electronic'],
      isFriend: true,
      createdAt: '2026-08-01T10:00:00.000Z',
    },
    {
      description:
        'Returns an empty body when the user is not found. `email`/`phoneNumber` are null when hidden by privacy; ' +
        'bio, dateOfBirth, musicPreferences, liked* and genres are omitted when profilePrivacy hides them. ' +
        '`isFriend` = that user has added you as a friend. Your own username returns your full profile.',
    },
  )
  async findOne(@CurrentUser() user: User, @Param('userName') userName: string) {
    const found = await this.users.findByUserName(userName, user.id);
    return found ? this.users.toPublicProfile(found, user.id) : null;
  }
}
