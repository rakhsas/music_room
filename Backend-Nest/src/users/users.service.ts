import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { In, Repository } from 'typeorm';
import { User } from './user.entity';
import { CardInput, chargeCard, PREMIUM_PRICE } from './subscription';

const PROFILE_FIELDS = [
  'fullName',
  'userName',
  'avatar',
  'bio',
  'dateOfBirth',
  'phoneNumber',
  'profilePrivacy',
  'emailPrivacy',
  'phonePrivacy',
  'musicPreferences',
  'likedArtists',
  'likedAlbums',
  'likedSongs',
  'genres',
] as const;

@Injectable()
export class UsersService {
  constructor(
    @InjectRepository(User) private readonly repo: Repository<User>,
  ) {}

  findByEmail(email: string) {
    return this.repo.findOne({ where: { email: email.trim().toLowerCase() } });
  }

  async findByUserName(userName: string, excludeUserId?: number) {
    const query = this.repo
      .createQueryBuilder('u')
      .where('LOWER(u.userName) = LOWER(:userName)', { userName })
      .andWhere('u.isActive = true');

    if (excludeUserId) {
      query.andWhere('u.id != :id', { id: excludeUserId });
    }
    return await query.getOne() ?? null;
  }

  findById(id: number) {
    return this.repo.findOne({ where: { id } });
  }

  findBySocialId(field: 'googleId', value: string) {
    return this.repo.findOne({ where: { [field]: value } as any });
  }

  async findAllExcept(userId: number) {
    // select() whitelists columns - without it this would leak the bcrypt
    // password hash and OTP fields to any authenticated caller.
    const users = await this.repo
      .createQueryBuilder('u')
      .select(['u.id', 'u.fullName', 'u.userName', 'u.email', 'u.avatar', 'u.emailPrivacy', 'u.friendIds'])
      .where('u.id != :id', { id: userId })
      .andWhere('u.isActive = true')
      .getMany();
    return users.map((u) => ({
      id: u.id,
      fullName: u.fullName,
      userName: u.userName,
      email: this.canSee(u, u.emailPrivacy, userId) ? u.email : null,
      avatar: u.avatar,
    }));
  }

  // "public" = anyone, "friends" = ids in owner.friendIds, "private" = owner only.
  private canSee(owner: User, level: string, viewerId: number) {
    if (owner.id === viewerId || level === 'public') return true;
    return level === 'friends' && (owner.friendIds ?? []).includes(viewerId);
  }

  // What another user sees: profilePrivacy gates the "about me" block, email/phone have
  // their own levels. Account internals (googleId, subscription) are never shown to others.
  toPublicProfile(user: User, viewerId: number) {
    if (user.id === viewerId) return this.toProfile(user);
    const profile = this.canSee(user, user.profilePrivacy, viewerId);
    return {
      id: user.id,
      fullName: user.fullName,
      userName: user.userName,
      avatar: user.avatar,
      email: this.canSee(user, user.emailPrivacy, viewerId) ? user.email : null,
      phoneNumber: this.canSee(user, user.phonePrivacy, viewerId) ? user.phoneNumber : null,
      ...(profile && {
        bio: user.bio,
        dateOfBirth: user.dateOfBirth,
        musicPreferences: user.musicPreferences,
        likedArtists: user.likedArtists,
        likedAlbums: user.likedAlbums,
        likedSongs: user.likedSongs,
        genres: user.genres,
      }),
      isFriend: (user.friendIds ?? []).includes(viewerId),
      createdAt: user.createdAt,
    };
  }

  async friends(user: User) {
    if (!user.friendIds.length) return [];
    const friends = await this.repo.find({ where: { id: In(user.friendIds) } });
    return friends.map((f) => ({ id: f.id, fullName: f.fullName, userName: f.userName, avatar: f.avatar }));
  }

  async addFriend(user: User, userName: string) {
    const friend = await this.findByUserName(userName, user.id);
    if (!friend) throw new NotFoundException('User not found');
    if (!user.friendIds.includes(friend.id)) {
      user.friendIds = [...user.friendIds, friend.id];
      await this.repo.save(user);
    }
    return { message: `${friend.fullName} can now see your friends-only info` };
  }

  async removeFriend(user: User, friendId: number) {
    user.friendIds = user.friendIds.filter((id) => id !== friendId);
    await this.repo.save(user);
    return { message: 'Friend removed' };
  }

  async generateUniqueUserName(base: string) {
    const cleaned = base.toLowerCase().replace(/[^a-z0-9_]/g, '') || 'user';
    let candidate = cleaned;
    let suffix = 0;
    while (await this.findByUserName(candidate)) {
      suffix += 1;
      candidate = `${cleaned}${suffix}`;
    }
    return candidate;
  }

  create(data: Partial<User> & { email: string; fullName: string; userName: string }) {
    const user = this.repo.create({
      ...data,
      email: data.email.trim().toLowerCase(),
    });
    return this.repo.save(user);
  }

  save(user: User) {
    return this.repo.save(user);
  }

  toProfile(user: User) {
    return {
      id: user.id,
      email: user.email,
      fullName: user.fullName,
      userName: user.userName,
      avatar: user.avatar,
      bio: user.bio,
      dateOfBirth: user.dateOfBirth,
      phoneNumber: user.phoneNumber,
      profilePrivacy: user.profilePrivacy,
      emailPrivacy: user.emailPrivacy,
      phonePrivacy: user.phonePrivacy,
      googleId: user.googleId,
      subscriptionType: user.subscriptionType,
      isPremium: user.isPremium,
      isSubscribed: user.isSubscribed,
      premiumSince: user.premiumSince,
      card: user.cardLast4 ? { brand: user.cardBrand, last4: user.cardLast4 } : null,
      plan: PREMIUM_PRICE,
      musicPreferences: user.musicPreferences,
      likedArtists: user.likedArtists,
      likedAlbums: user.likedAlbums,
      likedSongs: user.likedSongs,
      genres: user.genres,
      createdAt: user.createdAt,
    };
  }

  async updateProfile(user: User, data: Record<string, unknown>) {
    // Unique column - without this check a taken name surfaces as a raw 500.
    if (typeof data.userName === 'string' && (await this.findByUserName(data.userName, user.id))) {
      throw new BadRequestException('Username already taken');
    }
    for (const field of PROFILE_FIELDS) {
      if (data[field] !== undefined) (user as any)[field] = data[field];
    }
    await this.repo.save(user);
    return this.toProfile(user);
  }

  // Upgrading goes through the simulated checkout (subscription.ts); cancelling needs no
  // payment and forgets the stored card. Content created while premium (4+ playlists,
  // private ones) is kept - the limits only apply to creating more.
  async setSubscription(user: User, subscriptionType: 'free' | 'premium', card?: CardInput) {
    if (subscriptionType === user.subscriptionType) return this.toProfile(user);

    if (subscriptionType === 'premium') {
      if (!card) throw new BadRequestException('A payment card is required to upgrade to Premium');
      const charged = chargeCard(card);
      user.cardBrand = charged.brand;
      user.cardLast4 = charged.last4;
      user.premiumSince = new Date();
    } else {
      user.cardBrand = null;
      user.cardLast4 = null;
      user.premiumSince = null;
    }
    user.subscriptionType = subscriptionType;
    user.isPremium = subscriptionType === 'premium';
    user.isSubscribed = user.isPremium;
    await this.repo.save(user);
    return this.toProfile(user);
  }
}
