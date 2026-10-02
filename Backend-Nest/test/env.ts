// Loaded before every test file (jest "setupFiles") and by global-setup.ts.
// Env vars already set win over the root .env (dotenv never overrides), so this
// pins tests to a throwaway database - they never touch the dev "musicroom" data.
export const TEST_DATABASE_URL =
  process.env.TEST_DATABASE_URL ?? 'postgresql://postgres:postgres123@localhost:5433/musicroom_test';

process.env.DATABASE_URL = TEST_DATABASE_URL;
process.env.SECRET_KEY ??= 'test-secret';
process.env.REDIS_URL ??= 'redis://localhost:6379/15';
process.env.JAMENDO_CLIENT_ID ??= 'test';
