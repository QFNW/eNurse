"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.CacheService = void 0;
const common_1 = require("@nestjs/common");
const config_1 = require("@nestjs/config");
const ioredis_1 = __importDefault(require("ioredis"));
let CacheService = class CacheService {
    configService;
    redis;
    constructor(configService) {
        this.configService = configService;
        this.redis = new ioredis_1.default({
            host: this.configService.get('redis.host') || 'localhost',
            port: this.configService.get('redis.port') || 6379,
            lazyConnect: true,
        });
        this.redis.connect().catch((err) => {
            console.warn(`Redis connection failed: ${err.message} — token blacklist disabled`);
        });
    }
    async blacklist(key, ttlSeconds) {
        try {
            await this.redis.set(`blacklist:${key}`, '1', 'EX', ttlSeconds);
        }
        catch {
        }
    }
    async isBlacklisted(key) {
        try {
            return (await this.redis.exists(`blacklist:${key}`)) === 1;
        }
        catch {
            if (process.env.NODE_ENV === 'production') {
                throw new Error('Redis unavailable — token validation failed');
            }
            return false;
        }
    }
    async onModuleDestroy() {
        await this.redis.quit();
    }
};
exports.CacheService = CacheService;
exports.CacheService = CacheService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [config_1.ConfigService])
], CacheService);
//# sourceMappingURL=cache.service.js.map