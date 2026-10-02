import { Body, Controller, Get, HttpCode, Post, Put, Query, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiBody, ApiQuery, ApiTags } from '@nestjs/swagger';
import { Throttle, ThrottlerGuard } from '@nestjs/throttler';
import { AuthService } from './auth.service';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';
import { VerifyEmailDto } from './dto/verify-email.dto';
import { ResendVerificationDto } from './dto/resend-verification.dto';
import {
  PasswordResetConfirmDto,
  PasswordResetRequestDto,
  PasswordResetVerifyOtpDto,
} from './dto/password-reset.dto';
import { SocialLoginDto } from './dto/social.dto';
import { UpdateProfileDto } from './dto/update-profile.dto';
import { SubscribeDto } from './dto/subscribe.dto';
import { RefreshTokenDto } from './dto/refresh-token.dto';
import { Public } from './decorators/public.decorator';
import { CurrentUser } from './decorators/current-user.decorator';
import { User } from '../users/user.entity';
import { UsersService } from '../users/users.service';
import { ApiDoc, msg } from '../utils/api-docs';

const AUTH_RESPONSE = {
  user: { id: 1, email: 'john@example.com', fullName: 'John Doe', userName: 'johndoe', avatar: 'default_avatar.png' },
  tokens: { access: '<JWT, valid 60 min>', refresh: '<JWT, valid 7 days>' },
  message: 'Login successful',
};

const PROFILE = {
  id: 1,
  email: 'john@example.com',
  fullName: 'John Doe',
  userName: 'johndoe',
  avatar: 'default_avatar.png',
  bio: 'Music lover and playlist curator.',
  dateOfBirth: '1995-06-15',
  phoneNumber: '+33612345678',
  profilePrivacy: 'public',
  emailPrivacy: 'friends',
  phonePrivacy: 'private',
  googleId: null,
  subscriptionType: 'premium',
  isPremium: true,
  isSubscribed: true,
  premiumSince: '2026-09-01T10:00:00.000Z',
  card: { brand: 'Visa', last4: '4242' },
  plan: { amount: 4.99, currency: 'EUR', interval: 'month' },
  musicPreferences: { favorite_genre: 'rock' },
  likedArtists: ['Daft Punk'],
  likedAlbums: ['Discovery'],
  likedSongs: ['One More Time'],
  genres: ['rock', 'electronic'],
  createdAt: '2026-08-01T10:00:00.000Z',
};

const TOO_MANY = { 429: 'ThrottlerException: Too Many Requests (max 5 per minute)' };

// Auth lives under /api/users/ (login, create, profile, password-reset, social-login) because
// those are the paths the mobile client has hardcoded (see App/.../NetworkConfig.kt).
@ApiTags('auth')
@Controller('users')
@UseGuards(ThrottlerGuard)
export class AuthController {
  constructor(
    private readonly auth: AuthService,
    private readonly users: UsersService,
  ) {}

  @Public()
  @Post('create')
  @ApiDoc(
    'Register a new account',
    {
      id: 1,
      email: 'john@example.com',
      fullName: 'John Doe',
      userName: 'johndoe',
      message: 'Registration successful. Please check your email to verify your account.',
    },
    {
      status: 201,
      description: 'Account is inactive until the emailed verification link is opened.',
      errors: { 400: 'Email already exists | Username already taken' },
    },
  )
  register(@Body() dto: RegisterDto) {
    return this.auth.register(dto);
  }

  @Public()
  @Get('verify-email')
  @ApiQuery({ name: 'token', description: 'Token from the emailed link' })
  @ApiDoc('Verify email (link opened from the email)', msg('Email verified successfully! You can now log in to your account.'), {
    errors: { 400: 'Invalid or expired token' },
  })
  verifyEmailGet(@Query('token') token: string) {
    return this.auth.verifyEmail(token);
  }

  @Public()
  @Post('verify-email')
  @ApiDoc('Verify email (token in body)', msg('Email verified successfully! You can now log in to your account.'), {
    status: 201,
    errors: { 400: 'Invalid or expired token' },
  })
  verifyEmailPost(@Body() dto: VerifyEmailDto) {
    return this.auth.verifyEmail(dto.token);
  }

  @Public()
  @Throttle({ default: { limit: 5, ttl: 60_000 } })
  @Post('resend-verification-email')
  @ApiDoc('Resend the verification email', msg('Verification email sent successfully'), {
    status: 201,
    errors: { 400: 'User with this email does not exist | Email is already verified', ...TOO_MANY },
  })
  resendVerificationEmail(@Body() dto: ResendVerificationDto) {
    return this.auth.resendVerificationEmail(dto.email);
  }

  @Public()
  @Throttle({ default: { limit: 5, ttl: 60_000 } })
  @HttpCode(200)
  @Post('login')
  @ApiDoc('Log in with email + password', AUTH_RESPONSE, {
    description: 'Send `tokens.access` as `Authorization: Bearer <token>` on protected routes.',
    errors: { 401: 'Invalid email or password | Account is not verified. Please check your email.', ...TOO_MANY },
  })
  login(@Body() dto: LoginDto) {
    return this.auth.login(dto);
  }

  // @Public so it accepts either a bearer token (web) or just { refresh_token } in the body
  // (Android sends only that). Either way it revokes all of that user's tokens.
  @Public()
  @HttpCode(200)
  @Post('logout')
  @ApiBearerAuth()
  @ApiBody({ required: false, schema: { example: { refresh_token: '<refresh JWT>' } } })
  @ApiDoc('Log out everywhere', msg('Successfully logged out'), {
    description: 'Send a bearer token OR `refresh_token` in the body. Revokes every access/refresh token of the user.',
    errors: { 401: 'A bearer token or refresh_token is required' },
  })
  logout(@CurrentUser() user: User | undefined, @Body('refresh_token') refreshToken?: string) {
    return this.auth.logout(user, typeof refreshToken === 'string' ? refreshToken : undefined);
  }

  @Public()
  @HttpCode(200)
  @Post('token/refresh')
  @ApiDoc('Get a new access + refresh token pair', { ...AUTH_RESPONSE, message: 'Token refreshed' }, {
    errors: { 401: 'Invalid or expired refresh token' },
  })
  refresh(@Body() dto: RefreshTokenDto) {
    return this.auth.refresh(dto.refreshToken);
  }

  @Public()
  @Throttle({ default: { limit: 5, ttl: 60_000 } })
  @Post('password-reset')
  @ApiDoc('Password reset step 1: email a 6-digit OTP', msg('Password reset OTP sent to your email'), {
    status: 201,
    description: 'OTP is valid 10 minutes.',
    errors: { 400: 'User with this email does not exist', ...TOO_MANY },
  })
  requestPasswordReset(@Body() dto: PasswordResetRequestDto) {
    return this.auth.requestPasswordReset(dto.email);
  }

  @Public()
  @Throttle({ default: { limit: 5, ttl: 60_000 } })
  @Post('password-reset-verify-otp')
  @ApiDoc('Password reset step 2: verify the OTP', msg('OTP verified successfully. You can now set your new password.'), {
    status: 201,
    errors: {
      400: 'No OTP found. Please request a password reset. | The OTP has expired. Please request a new one. | Invalid OTP. Please try again.',
      ...TOO_MANY,
    },
  })
  verifyPasswordResetOtp(@Body() dto: PasswordResetVerifyOtpDto) {
    return this.auth.verifyPasswordResetOtp(dto.email, dto.otp);
  }

  @Public()
  @Throttle({ default: { limit: 5, ttl: 60_000 } })
  @Post('password-reset-confirm')
  @ApiDoc('Password reset step 3: set the new password', msg('Password reset successful! You can now log in with your new password.'), {
    status: 201,
    errors: { 400: 'OTP not verified. Please verify the OTP first. | Invalid OTP. | Your OTP session has expired.', ...TOO_MANY },
  })
  confirmPasswordReset(@Body() dto: PasswordResetConfirmDto) {
    return this.auth.confirmPasswordReset(dto.email, dto.otp, dto.password);
  }

  @Public()
  @Throttle({ default: { limit: 5, ttl: 60_000 } })
  @HttpCode(200)
  @Post('social-login')
  @ApiDoc('Log in / sign up with Google', { ...AUTH_RESPONSE, message: 'google login successful' }, {
    description: 'Creates the account on first login (or links it to an existing account with the same email).',
    errors: { 400: 'Invalid Google token | Google did not share an email address', ...TOO_MANY },
  })
  socialLogin(@Body() dto: SocialLoginDto) {
    console.log(`Received request to login with ${dto.provider} account`);
    return this.auth.socialLogin(dto);
  }

  @ApiBearerAuth()
  @Post('social-link')
  @ApiDoc('Link a Google account to the current user', msg('Google account linked successfully'), {
    status: 201,
    errors: { 400: 'Invalid Google token', 409: 'This Google account is already linked to another user' },
  })
  socialLink(@CurrentUser() user: User, @Body() dto: SocialLoginDto) {
    return this.auth.linkSocialAccount(user, dto);
  }

  @ApiBearerAuth()
  @Get('profile')
  @ApiDoc('Get my full profile', PROFILE, { description: '`card` is null when no card is stored.' })
  profile(@CurrentUser() user: User) {
    console.log(`Fetching profile for user ${user.id}`);
    return this.users.toProfile(user);
  }

  @ApiBearerAuth()
  @Put('profile/update')
  @ApiDoc('Update my profile (only sent fields change)', PROFILE, { errors: { 400: 'Username already taken' } })
  updateProfile(@CurrentUser() user: User, @Body() dto: UpdateProfileDto) {
    return this.users.updateProfile(user, { ...dto });
  }

  @ApiBearerAuth()
  @Post('subscribe')
  @ApiDoc('Switch plan (free / premium)', PROFILE, {
    status: 201,
    description: 'Returns the updated profile. Premium needs a card (simulated checkout; 4000 0000 0000 0002 is always declined).',
    errors: {
      400: 'A payment card is required to upgrade to Premium | Card number is invalid | Card has expired | Security code (CVC) is invalid',
      402: 'Your card was declined',
    },
  })
  subscribe(@CurrentUser() user: User, @Body() dto: SubscribeDto) {
    return this.users.setSubscription(user, dto.subscriptionType, dto.card);
  }
}
