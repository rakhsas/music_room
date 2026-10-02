import { SetMetadata } from '@nestjs/common';

export const IS_PUBLIC_KEY = 'isPublic';
// Every route requires a token by default (global JwtAuthGuard); @Public() opts a route out.
export const Public = () => SetMetadata(IS_PUBLIC_KEY, true);
