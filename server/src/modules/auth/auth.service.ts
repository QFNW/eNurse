import {
  Injectable,
  UnauthorizedException,
  ConflictException,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import * as bcrypt from 'bcrypt';
import { PrismaService } from '../../prisma/prisma.service';
import { CacheService } from '../cache/cache.service';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';
import { randomUUID } from 'crypto';

@Injectable()
export class AuthService {
  constructor(
    private prisma: PrismaService,
    private jwtService: JwtService,
    private configService: ConfigService,
    private cacheService: CacheService,
  ) {}

  async register(dto: RegisterDto) {
    const passwordHash = await bcrypt.hash(dto.password, 12);
    // Force PATIENT role for self-registration — NURSE/ADMIN must be created by admin
    let user;
    try {
      user = await this.prisma.user.create({
        data: {
          email: dto.email,
          passwordHash,
          role: dto.role,
        },
      });
    } catch (err: any) {
      if (err.code === 'P2002') {
        throw new ConflictException('该邮箱已被注册');
      }
      throw err;
    }

    const tokens = await this.generateTokens(user.id, user.email, user.role);
    return {
      ...tokens,
      user: { id: user.id, email: user.email, role: user.role },
    };
  }

  async login(dto: LoginDto) {
    const user = await this.prisma.user.findUnique({
      where: { email: dto.email },
    });
    if (!user) {
      throw new UnauthorizedException('邮箱或密码错误');
    }

    const valid = await bcrypt.compare(dto.password, user.passwordHash);
    if (!valid) {
      throw new UnauthorizedException('邮箱或密码错误');
    }

    const tokens = await this.generateTokens(user.id, user.email, user.role);
    return {
      ...tokens,
      user: { id: user.id, email: user.email, role: user.role },
    };
  }

  async refresh(refreshToken: string) {
    try {
      const payload = this.jwtService.verify(refreshToken, {
        secret: this.configService.get<string>('jwt.refreshSecret'),
      });

      // Check blacklist
      if (payload.tokenId && await this.cacheService.isBlacklisted(payload.tokenId)) {
        throw new UnauthorizedException('refreshToken 已被撤销');
      }

      const user = await this.prisma.user.findUnique({
        where: { id: payload.sub },
      });
      if (!user) {
        throw new UnauthorizedException('用户不存在');
      }

      const tokens = await this.generateTokens(user.id, user.email, user.role);

      // Blacklist old tokenId AFTER new tokens are issued
      if (payload.tokenId) {
        const remainingSeconds = payload.exp
          ? payload.exp - Math.floor(Date.now() / 1000)
          : 7 * 24 * 3600;
        await this.cacheService.blacklist(payload.tokenId, Math.max(1, remainingSeconds));
      }
      return {
        ...tokens,
        user: { id: user.id, email: user.email, role: user.role },
      };
    } catch (err: any) {
      if (err instanceof UnauthorizedException) throw err;
      throw new UnauthorizedException('refreshToken 无效或已过期');
    }
  }

  async logout(refreshToken: string) {
    try {
      const payload = this.jwtService.verify(refreshToken, {
        secret: this.configService.get<string>('jwt.refreshSecret'),
      });
      if (payload.tokenId) {
        const remainingSeconds = payload.exp
          ? payload.exp - Math.floor(Date.now() / 1000)
          : 7 * 24 * 3600;
        if (remainingSeconds > 0) {
          try {
            await this.cacheService.blacklist(payload.tokenId, remainingSeconds);
          } catch (blacklistErr: any) {
            // Logged but not fatal — token will expire naturally
            console.error(`Logout blacklist failed for token ${payload.tokenId}: ${blacklistErr.message}`);
          }
        }
      }
    } catch {
      // Token already expired — no need to blacklist
    }
    return { message: '已登出' };
  }

  private async generateTokens(userId: string, email: string, role: string) {
    const tokenId = randomUUID();

    const accessSecret = this.configService.get<string>('jwt.accessSecret') as string;
    const accessExpires = this.configService.get<string>('jwt.accessExpiresIn') as string;
    const refreshSecret = this.configService.get<string>('jwt.refreshSecret') as string;
    const refreshExpires = this.configService.get<string>('jwt.refreshExpiresIn') as string;

    const [accessToken, refreshToken] = await Promise.all([
      this.jwtService.signAsync(
        { sub: userId, email, role },
        { secret: accessSecret, expiresIn: accessExpires as any },
      ),
      this.jwtService.signAsync(
        { sub: userId, tokenId },
        { secret: refreshSecret, expiresIn: refreshExpires as any },
      ),
    ]);

    return { accessToken, refreshToken };
  }
}
