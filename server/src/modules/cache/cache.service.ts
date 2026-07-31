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
    } catch (err: any) {
      // Fail open in production — log but don't block all token refresh
      console.error(`Redis blacklist check failed for key ${key}: ${err.message}`);
      return false;
    }
  }

  /** Store verification code with TTL in seconds */
  async setCode(email: string, code: string, ttlSeconds: number): Promise<void> {
    try {
      await this.redis.set(`email_code:${email}`, code, 'EX', ttlSeconds);
    } catch (err: any) {
      console.error(`Redis setCode failed: ${err.message}`);
    }
  }

  /** Get stored verification code */
  async getCode(email: string): Promise<string | null> {
    try {
      return await this.redis.get(`email_code:${email}`);
    } catch (err: any) {
      console.error(`Redis getCode failed: ${err.message}`);
      return null;
    }
  }

  /** Delete verification code after successful verification */
  async deleteCode(email: string): Promise<void> {
    try {
      await this.redis.del(`email_code:${email}`);
    } catch (err: any) {
      console.error(`Redis deleteCode failed: ${err.message}`);
    }
  }

  async onModuleDestroy() {
    await this.redis.quit();
  }
}
