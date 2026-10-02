import { Client } from 'pg';
import { TEST_DATABASE_URL } from './env';

// Creates the test database on first run; TypeORM's synchronize builds the tables.
export default async function globalSetup() {
  const url = new URL(TEST_DATABASE_URL);
  const dbName = url.pathname.slice(1);
  url.pathname = '/postgres';

  const client = new Client({ connectionString: url.toString() });
  await client.connect();
  const { rowCount } = await client.query('SELECT 1 FROM pg_database WHERE datname = $1', [dbName]);
  if (!rowCount) await client.query(`CREATE DATABASE "${dbName}"`);
  await client.end();
}
