import { ConfigService } from '@nestjs/config';
import { CacheService } from '../cache/cache.service';
export declare class EmailService {
    private configService;
    private cacheService;
    private transporter;
    constructor(configService: ConfigService, cacheService: CacheService);
    sendVerificationCode(email: string): Promise<void>;
    verifyCode(email: string, code: string): Promise<boolean>;
    consumeCode(email: string, code: string): Promise<boolean>;
}
