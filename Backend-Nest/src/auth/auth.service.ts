import {
  BadRequestException,
  ConflictException,
  Injectable,
  UnauthorizedException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { HttpService } from '@nestjs/axios';
import { JwtService } from '@nestjs/jwt';
import * as bcrypt from 'bcryptjs';
import { randomInt } from 'crypto';
import { firstValueFrom } from 'rxjs';
import { UsersService } from '../users/users.service';
import { User } from '../users/user.entity';
import { MailService } from '../mail/mail.service';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';
import { SocialLoginDto } from './dto/social.dto';

const OTP_TTL_MS = 10 * 60 * 1000;
const OTP_CONFIRM_TTL_MS = 15 * 60 * 1000;

@Injectable()
export class AuthService {
  constructor(
    private readonly users: UsersService,
    private readonly jwt: JwtService,
    private readonly mail: MailService,
    private readonly http: HttpService,
    private readonly config: ConfigService,
  ) {}

  async register(dto: RegisterDto) {
    if (await this.users.findByEmail(dto.email)) {
      // 400, not 409: the mobile client's response parsing only treats 400 as a
      // "show this message" error.
      throw new BadRequestException('Email already exists');
    }
    if (await this.users.findByUserName(dto.userName)) {
      throw new BadRequestException('Username already taken');
    }
    const password = await bcrypt.hash(dto.password, 10);
    const user = await this.users.create({ ...dto, password, isActive: false, isVerified: false });
    await this.sendVerificationEmail(user);

    return {
      id: user.id,
      email: user.email,
      fullName: user.fullName,
      userName: user.userName,
      message: 'Registration successful. Please check your email to verify your account.',
    };
  }

  async resendVerificationEmail(email: string) {
    const user = await this.users.findByEmail(email);
    if (!user) throw new BadRequestException('User with this email does not exist');
    if (user.isVerified) throw new BadRequestException('Email is already verified');

    await this.sendVerificationEmail(user);
    return { message: 'Verification email sent successfully' };
  }

  private async sendVerificationEmail(user: User) {
    const token = this.jwt.sign(
      { sub: user.id, action: 'email_verification' },
      { expiresIn: '24h' },
    );
    // Tolerate DEFAULT_API_URL with or without a trailing slash (a missing/doubled slash broke these links before).
    const base = (this.config.get<string>('DEFAULT_API_URL') ?? '').replace(/\/+$/, '');
    const verifyUrl = `${base}/api/users/verify-email?token=${token}`;
    await this.mail.send(
      user.email,
      'Verify your MusicRoom account',
      `Please click the link to verify your email: ${verifyUrl}`,
    );
  }

  async verifyEmail(token: string) {
    const payload = this.verifyToken(token, 'email_verification');
    const user = await this.users.findById(payload.sub);
    if (!user) throw new BadRequestException('Invalid verification link');

    if (user.isVerified) return { message: 'Email already verified' };
    user.isActive = true;
    user.isVerified = true;
    await this.users.save(user);
    return { message: 'Email verified successfully! You can now log in to your account.' };
  }

  async login(dto: LoginDto) {
    const user = await this.users.findByEmail(dto.email);
    if (!user || !user.password || !(await bcrypt.compare(dto.password, user.password))) {
      throw new UnauthorizedException('Invalid email or password');
    }
    if (!user.isActive) {
      throw new UnauthorizedException('Account is not verified. Please check your email.');
    }
    return this.toAuthResponse(user);
  }

  async requestPasswordReset(email: string) {
    const user = await this.users.findByEmail(email);
    if (!user) throw new BadRequestException('User with this email does not exist');

    const otp = randomInt(0, 1_000_000).toString().padStart(6, '0');
    user.passwordResetOtp = otp;
    user.passwordResetOtpCreatedAt = new Date();
    user.passwordResetOtpVerified = false;
    await this.users.save(user);

    await this.mail.send(
      user.email,
      'MusicRoom Password Reset OTP',
      `Your password reset OTP is: ${otp}\n\nThis OTP will expire in 10 minutes.`,
    );
    return { message: 'Password reset OTP sent to your email' };
  }

  async verifyPasswordResetOtp(email: string, otp: string) {
    const user = await this.users.findByEmail(email);
    if (!user?.passwordResetOtp) {
      throw new BadRequestException('No OTP found. Please request a password reset.');
    }
    this.assertOtpFresh(user, OTP_TTL_MS, 'The OTP has expired. Please request a new one.');
    if (user.passwordResetOtp !== otp) throw new BadRequestException('Invalid OTP. Please try again.');

    user.passwordResetOtpVerified = true;
    await this.users.save(user);
    return { message: 'OTP verified successfully. You can now set your new password.' };
  }

  async confirmPasswordReset(email: string, otp: string, password: string) {
    const user = await this.users.findByEmail(email);
    if (!user?.passwordResetOtpVerified) {
      throw new BadRequestException('OTP not verified. Please verify the OTP first.');
    }
    if (user.passwordResetOtp !== otp) throw new BadRequestException('Invalid OTP.');
    this.assertOtpFresh(user, OTP_CONFIRM_TTL_MS, 'Your OTP session has expired. Please request a new reset.');

    user.password = await bcrypt.hash(password, 10);
    user.passwordResetOtp = null;
    user.passwordResetOtpCreatedAt = null;
    user.passwordResetOtpVerified = false;
    await this.users.save(user);
    return { message: 'Password reset successful! You can now log in with your new password.' };
  }

  async socialLogin(dto: SocialLoginDto) {
    const profile = await this.fetchGoogleProfile(dto.accessToken);

    let user = await this.users.findBySocialId('googleId', profile.id);
    if (!user) {
      if (!profile.email) {
        throw new BadRequestException(
          'Google did not share an email address; please register with email/password instead',
        );
      }
      user = await this.users.findByEmail(profile.email);
      if (user) {
        user.googleId = profile.id;
      } else {
        const fullName = profile.name || profile.email.split('@')[0];
        const userName = await this.users.generateUniqueUserName(profile.email.split('@')[0]);
        user = await this.users.create({
          email: profile.email,
          fullName,
          userName,
          password: null,
          googleId: profile.id,
        } as Partial<User> & { email: string; fullName: string; userName: string });
      }
    }

    user.isActive = true;
    user.isVerified = true;
    await this.users.save(user);

    return this.toAuthResponse(user, 'google login successful');
  }

  async linkSocialAccount(user: User, dto: SocialLoginDto) {
    const profile = await this.fetchGoogleProfile(dto.accessToken);

    const existing = await this.users.findBySocialId('googleId', profile.id);
    if (existing && existing.id !== user.id) {
      throw new ConflictException('This Google account is already linked to another user');
    }

    user.googleId = profile.id;
    await this.users.save(user);
    return { message: 'Google account linked successfully' };
  }

  private async fetchGoogleProfile(token: string) {
    // Google tokens can be an ID token or an access token - try ID token first.
    try {
      const { data } = await firstValueFrom(
        this.http.get('https://oauth2.googleapis.com/tokeninfo', { params: { id_token: token } }),
      );
      console.log('Google tokeninfo response:', data);
      if (data?.sub) return { id: data.sub as string, email: data.email as string | undefined, name: data.name as string | undefined };
    } catch {
      console.log('Google token is not an ID token, trying as access token...');
      // fall through to access-token path
    }

    try {
      const { data: tokenInfo } = await firstValueFrom(
        this.http.get('https://oauth2.googleapis.com/tokeninfo', { params: { access_token: token } }),
      );
      if (tokenInfo?.scope) {
        const { data: userInfo } = await firstValueFrom(
          this.http.get('https://www.googleapis.com/oauth2/v2/userinfo', { params: { access_token: token } }),
        );
        if (userInfo?.id) return { id: userInfo.id as string, email: userInfo.email as string | undefined, name: userInfo.name as string | undefined };
      }
    } catch {
      // handled below
    }

    throw new BadRequestException('Invalid Google token');
  }

  // Rotates both tokens; the old refresh token stays valid until it expires or the user
  // logs out (no per-token store - tokenVersion revokes all of a user's tokens at once).
  async refresh(refreshToken: string) {
    const user = await this.userFromRefreshToken(refreshToken);
    return this.toAuthResponse(user, 'Token refreshed');
  }

  // Logs the user out everywhere: every access/refresh token issued so far stops working.
  async logout(user: User | undefined, refreshToken?: string) {
    const target = user ?? (refreshToken ? await this.userFromRefreshToken(refreshToken) : undefined);
    if (!target) throw new UnauthorizedException('A bearer token or refresh_token is required');
    target.tokenVersion += 1;
    await this.users.save(target);
    return { message: 'Successfully logged out' };
  }

  private async userFromRefreshToken(token: string) {
    let payload: { sub: number; type?: string; ver?: number };
    try {
      payload = this.jwt.verify(token);
    } catch {
      throw new UnauthorizedException('Invalid or expired refresh token');
    }
    const user = payload.type === 'refresh' ? await this.users.findById(payload.sub) : null;
    if (!user || (payload.ver ?? 0) !== user.tokenVersion) {
      throw new UnauthorizedException('Invalid or expired refresh token');
    }
    return user;
  }

  private assertOtpFresh(user: User, ttlMs: number, message: string) {
    const createdAt = user.passwordResetOtpCreatedAt;
    if (createdAt && Date.now() - new Date(createdAt).getTime() > ttlMs) {
      user.passwordResetOtp = null;
      user.passwordResetOtpCreatedAt = null;
      user.passwordResetOtpVerified = false;
      this.users.save(user);
      throw new BadRequestException(message);
    }
  }

  private verifyToken(token: string, expectedAction: string): { sub: number; action: string } {
    try {
      const payload = this.jwt.verify(token);
      if (payload.action !== expectedAction) throw new Error('wrong action');
      return payload;
    } catch {
      throw new BadRequestException('Invalid or expired token');
    }
  }

  private toAuthResponse(user: User, message = 'Login successful') {
    // The mobile client (AuthApiService.kt) parses response.tokens.access/.refresh,
    // not a flat accessToken field - keep this shape.
    return {
      user: { id: user.id, email: user.email, fullName: user.fullName, userName: user.userName, avatar: user.avatar },
      tokens: {
        access: this.jwt.sign({ sub: user.id, type: 'access', ver: user.tokenVersion }, { expiresIn: '60m' }),
        refresh: this.jwt.sign({ sub: user.id, type: 'refresh', ver: user.tokenVersion }, { expiresIn: '7d' }),
      },
      message,
    };
  }
}
