import { Logger } from '@nestjs/common';

const logger = new Logger('Configuration');

/**
 * Validate that a required secret is configured.
 * In production, refuses to use inline defaults.
 * Does NOT reject user-configured values that happen to contain 'dev-'.
 */
function requireSecret(
  name: string,
  envValue: string | undefined,
  defaultValue: string,
): string {
  const value = envValue || defaultValue;
  const isExplicitlySet = envValue !== undefined;

  if (!isExplicitlySet && process.env.NODE_ENV === 'production') {
    throw new Error(
      `Security: ${name} is not set. ` +
      'It must be configured via environment variable in production.',
    );
  }

  if (!isExplicitlySet) {
    logger.warn(
      `⚠ ${name} not set — using default (NOT safe for production)`,
    );
  }

  return value;
}

export default () => ({
  port: parseInt(process.env.PORT || '3000', 10),
  jwt: {
    accessSecret: requireSecret(
      'ACCESS_TOKEN_SECRET',
      process.env.ACCESS_TOKEN_SECRET,
      'dev-access-secret',
    ),
    accessExpiresIn: process.env.ACCESS_TOKEN_EXPIRES_IN || '15m',
    refreshSecret: requireSecret(
      'REFRESH_TOKEN_SECRET',
      process.env.REFRESH_TOKEN_SECRET,
      'dev-refresh-secret',
    ),
    refreshExpiresIn: process.env.REFRESH_TOKEN_EXPIRES_IN || '7d',
  },
  redis: {
    host: process.env.REDIS_HOST || 'localhost',
    port: parseInt(process.env.REDIS_PORT || '6379', 10),
  },
  admin: {
    passwordHash: process.env.ADMIN_PASSWORD_HASH || '',
  },
});
