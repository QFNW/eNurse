export default () => ({
  port: parseInt(process.env.PORT || '3000', 10),
  jwt: {
    accessSecret: process.env.ACCESS_TOKEN_SECRET || 'dev-access-secret',
    accessExpiresIn: process.env.ACCESS_TOKEN_EXPIRES_IN || '15m',
    refreshSecret:
      process.env.REFRESH_TOKEN_SECRET || 'dev-refresh-secret',
    refreshExpiresIn: process.env.REFRESH_TOKEN_EXPIRES_IN || '7d',
  },
  redis: {
    host: process.env.REDIS_HOST || 'localhost',
    port: parseInt(process.env.REDIS_PORT || '6379', 10),
  },
  admin: {
    password: process.env.ADMIN_PASSWORD || 'admin_enurse_2026',
  },
});
