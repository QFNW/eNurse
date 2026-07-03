import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { PrismaService } from '../../prisma/prisma.service';
import { CacheService } from '../cache/cache.service';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';
export declare class AuthService {
    private prisma;
    private jwtService;
    private configService;
    private cacheService;
    constructor(prisma: PrismaService, jwtService: JwtService, configService: ConfigService, cacheService: CacheService);
    register(dto: RegisterDto): Promise<{
        user: {
            id: string;
            email: string;
            role: import("../../../generated/prisma/enums").Role;
        };
        accessToken: string;
        refreshToken: string;
    }>;
    login(dto: LoginDto): Promise<{
        user: {
            id: string;
            email: string;
            role: import("../../../generated/prisma/enums").Role;
        };
        accessToken: string;
        refreshToken: string;
    }>;
    refresh(refreshToken: string): Promise<{
        user: {
            id: string;
            email: string;
            role: import("../../../generated/prisma/enums").Role;
        };
        accessToken: string;
        refreshToken: string;
    }>;
    logout(refreshToken: string): Promise<{
        message: string;
    }>;
    private generateTokens;
}
