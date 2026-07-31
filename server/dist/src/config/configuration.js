"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const common_1 = require("@nestjs/common");
const logger = new common_1.Logger('Configuration');
function requireSecret(name, envValue, defaultValue) {
    const isExplicitlySet = envValue !== undefined && envValue !== '';
    const value = isExplicitlySet ? envValue : defaultValue;
    if (!isExplicitlySet) {
        if (process.env.NODE_ENV === 'production') {
            throw new Error(`Security: ${name} is not set (empty or missing). ` +
                'It must be configured via environment variable in production.');
        }
        logger.warn(`⚠ ${name} not set — using default (NOT safe for production)`);
    }
    return value;
}
exports.default = () => ({
    port: parseInt(process.env.PORT || '3000', 10),
    jwt: {
        accessSecret: requireSecret('ACCESS_TOKEN_SECRET', process.env.ACCESS_TOKEN_SECRET, 'dev-access-secret'),
        accessExpiresIn: process.env.ACCESS_TOKEN_EXPIRES_IN || '15m',
        refreshSecret: requireSecret('REFRESH_TOKEN_SECRET', process.env.REFRESH_TOKEN_SECRET, 'dev-refresh-secret'),
        refreshExpiresIn: process.env.REFRESH_TOKEN_EXPIRES_IN || '7d',
    },
    redis: {
        host: process.env.REDIS_HOST || 'localhost',
        port: parseInt(process.env.REDIS_PORT || '6379', 10),
    },
    admin: {
        passwordHash: process.env.ADMIN_PASSWORD_HASH || '',
    },
    smtp: {
        host: process.env.SMTP_HOST || 'smtp.qq.com',
        port: parseInt(process.env.SMTP_PORT || '587', 10),
        user: process.env.SMTP_USER || '',
        pass: process.env.SMTP_PASS || '',
    },
});
//# sourceMappingURL=configuration.js.map