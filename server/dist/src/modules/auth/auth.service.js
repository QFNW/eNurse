"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.AuthService = void 0;
const common_1 = require("@nestjs/common");
const jwt_1 = require("@nestjs/jwt");
const config_1 = require("@nestjs/config");
const bcrypt = __importStar(require("bcrypt"));
const prisma_service_1 = require("../../prisma/prisma.service");
const cache_service_1 = require("../cache/cache.service");
const crypto_1 = require("crypto");
let AuthService = class AuthService {
    prisma;
    jwtService;
    configService;
    cacheService;
    constructor(prisma, jwtService, configService, cacheService) {
        this.prisma = prisma;
        this.jwtService = jwtService;
        this.configService = configService;
        this.cacheService = cacheService;
    }
    async register(dto) {
        const passwordHash = await bcrypt.hash(dto.password, 12);
        let user;
        try {
            user = await this.prisma.user.create({
                data: {
                    username: dto.username,
                    email: dto.email,
                    displayName: dto.displayName,
                    passwordHash,
                    role: dto.role,
                },
            });
        }
        catch (err) {
            if (err.code === 'P2002') {
                const msg = (err.meta?.target || []).includes('username')
                    ? '该用户名已被注册'
                    : '该邮箱已被注册';
                throw new common_1.ConflictException(msg);
            }
            throw err;
        }
        const tokens = await this.generateTokens(user.id, user.username, user.email, user.role);
        return {
            ...tokens,
            user: { id: user.id, username: user.username, email: user.email, role: user.role, displayName: user.displayName },
        };
    }
    async login(dto) {
        const isEmail = dto.login?.includes('@');
        const user = isEmail
            ? await this.prisma.user.findUnique({ where: { email: dto.login } })
            : await this.prisma.user.findUnique({ where: { username: dto.login } });
        if (!user) {
            throw new common_1.UnauthorizedException('用户名或密码错误');
        }
        const valid = await bcrypt.compare(dto.password, user.passwordHash);
        if (!valid) {
            throw new common_1.UnauthorizedException('用户名或密码错误');
        }
        const tokens = await this.generateTokens(user.id, user.username, user.email, user.role);
        return {
            ...tokens,
            user: { id: user.id, username: user.username, email: user.email, role: user.role, displayName: user.displayName },
        };
    }
    async refresh(refreshToken) {
        try {
            const payload = this.jwtService.verify(refreshToken, {
                secret: this.configService.get('jwt.refreshSecret'),
            });
            if (payload.tokenId && await this.cacheService.isBlacklisted(payload.tokenId)) {
                throw new common_1.UnauthorizedException('refreshToken 已被撤销');
            }
            const user = await this.prisma.user.findUnique({
                where: { id: payload.sub },
            });
            if (!user) {
                throw new common_1.UnauthorizedException('用户不存在');
            }
            const tokens = await this.generateTokens(user.id, user.username, user.email, user.role);
            if (payload.tokenId) {
                const remainingSeconds = payload.exp
                    ? payload.exp - Math.floor(Date.now() / 1000)
                    : 7 * 24 * 3600;
                await this.cacheService.blacklist(payload.tokenId, Math.max(1, remainingSeconds));
            }
            return {
                ...tokens,
                user: { id: user.id, username: user.username, email: user.email, role: user.role, displayName: user.displayName },
            };
        }
        catch (err) {
            if (err instanceof common_1.UnauthorizedException)
                throw err;
            throw new common_1.UnauthorizedException('refreshToken 无效或已过期');
        }
    }
    async logout(refreshToken) {
        try {
            const payload = this.jwtService.verify(refreshToken, {
                secret: this.configService.get('jwt.refreshSecret'),
            });
            if (payload.tokenId) {
                const remainingSeconds = payload.exp
                    ? payload.exp - Math.floor(Date.now() / 1000)
                    : 7 * 24 * 3600;
                if (remainingSeconds > 0) {
                    try {
                        await this.cacheService.blacklist(payload.tokenId, remainingSeconds);
                    }
                    catch (blacklistErr) {
                        console.error(`Logout blacklist failed for token ${payload.tokenId}: ${blacklistErr.message}`);
                    }
                }
            }
        }
        catch {
        }
        return { message: '已登出' };
    }
    async generateTokens(userId, username, email, role) {
        const tokenId = (0, crypto_1.randomUUID)();
        const accessSecret = this.configService.get('jwt.accessSecret');
        const accessExpires = this.configService.get('jwt.accessExpiresIn');
        const refreshSecret = this.configService.get('jwt.refreshSecret');
        const refreshExpires = this.configService.get('jwt.refreshExpiresIn');
        const [accessToken, refreshToken] = await Promise.all([
            this.jwtService.signAsync({ sub: userId, username, email, role }, { secret: accessSecret, expiresIn: accessExpires }),
            this.jwtService.signAsync({ sub: userId, tokenId }, { secret: refreshSecret, expiresIn: refreshExpires }),
        ]);
        return { accessToken, refreshToken };
    }
};
exports.AuthService = AuthService;
exports.AuthService = AuthService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService,
        jwt_1.JwtService,
        config_1.ConfigService,
        cache_service_1.CacheService])
], AuthService);
//# sourceMappingURL=auth.service.js.map