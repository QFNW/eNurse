import { Logger } from '@nestjs/common';

const logger = new Logger('Configuration');

function requireSecret(name: string, value: string | undefined): string {
  if (!value || value.startsWith('dev-')) {
    if (process.env.NODE_ENV === 'production') {
      throw new Error(
        `Security: ${name} is not set or uses a dev default. ` +
        'Set it in the environment for production.',
      );
    }
    logger.warn(
      `⚠ ${name} uses default/dev value — NOT safe for production`,
    );
  }
  return value || '';
}

export default () => ({
  port: parseInt(process.env.PORT || '3000', 10),
  jwt: {
    accessSecret: requireSecret(
      'ACCESS_TOKEN_SECRET',
      process.env.ACCESS_TOKEN_SECRET || 'dev-access-secret',
    ),
    accessExpiresIn: process.env.ACCESS_TOKEN_EXPIRES_IN || '15m',
    refreshSecret: requireSecret(
      'REFRESH_TOKEN_SECRET',
      process.env.REFRESH_TOKEN_SECRET || 'dev-refresh-secret',
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
