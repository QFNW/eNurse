import { OnModuleDestroy } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
export declare class CacheService implements OnModuleDestroy {
    private configService;
    private readonly redis;
    constructor(configService: ConfigService);
    blacklist(key: string, ttlSeconds: number): Promise<void>;
    isBlacklisted(key: string): Promise<boolean>;
    setCode(email: string, code: string, ttlSeconds: number): Promise<void>;
    getCode(email: string): Promise<string | null>;
    deleteCode(email: string): Promise<void>;
    onModuleDestroy(): Promise<void>;
}
