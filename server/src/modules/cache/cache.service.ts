import { Injectable, OnModuleDestroy } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import Redis from 'ioredis';

@Injectable()
export class CacheService implements OnModuleDestroy {
  private readonly redis: Redis;

  constructor(private configService: ConfigService) {
    this.redis = new Redis({
      host: this.configService.get<string>('redis.host') || 'localhost',
      port: this.configService.get<number>('redis.port') || 6379,
      lazyConnect: true,
    });
    this.redis.connect().catch((err) => {
      // Redis is optional for core functionality
      console.warn(`Redis connection failed: ${err.message} — token blacklist disabled`);
    });
  }

  /** Add tokenId to blacklist with TTL in seconds */
  async blacklist(key: string, ttlSeconds: number): Promise<void> {
    try {
      await this.redis.set(`blacklist:${key}`, '1', 'EX', ttlSeconds);
    } catch {
      // Best-effort: if Redis is down, token security is degraded but functional
    }
  }

  /** Check if a tokenId is blacklisted */
  async isBlacklisted(key: string): Promise<boolean> {
    try {
      return (await this.redis.exists(`blacklist:${key}`)) === 1;
    } catch {
      // Fail closed in production, open in dev
      if (process.env.NODE_ENV === 'production') {
        throw new Error('Redis unavailable — token validation failed');
      }
      return false;
    }
  }

  async onModuleDestroy() {
    await this.redis.quit();
  }
}
