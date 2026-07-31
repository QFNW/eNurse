import {
  Injectable,
  UnauthorizedException,
  ConflictException,
  BadRequestException,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import * as bcrypt from 'bcrypt';
import { PrismaService } from '../../prisma/prisma.service';
import { CacheService } from '../cache/cache.service';
import { EmailService } from './email.service';
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
    private emailService: EmailService,
  ) {}

  async sendCode(email: string) {
    // Check if email is already registered
    const existing = await this.prisma.user.findUnique({ where: { email } });
    if (existing) throw new ConflictException('该邮箱已被注册');

    await this.emailService.sendVerificationCode(email);
    return { message: '验证码已发送，5分钟内有效' };
  }

  async register(dto: RegisterDto) {
    // Verify code
    const codeValid = await this.emailService.consumeCode(dto.email, dto.code);
    if (!codeValid) throw new BadRequestException('验证码错误或已过期');

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
    } catch (err: any) {
      if (err.code === 'P2002') {
        const msg = (err.meta?.target as string[] || []).includes('username')
          ? '该用户名已被注册'
          : '该邮箱已被注册';
        throw new ConflictException(msg);
      }
      throw err;
    }

    const tokens = await this.generateTokens(user.id, user.username, user.email, user.role);
    return {
      ...tokens,
      user: { id: user.id, username: user.username, email: user.email, role: user.role, displayName: user.displayName },
    };
  }

  async login(dto: LoginDto) {
    // Support login by username or email (detect @ to distinguish)
    const isEmail = dto.login?.includes('@');
    const user = isEmail
      ? await this.prisma.user.findUnique({ where: { email: dto.login } })
      : await this.prisma.user.findUnique({ where: { username: dto.login } });

    if (!user) {
      throw new UnauthorizedException('用户名或密码错误');
    }

    const valid = await bcrypt.compare(dto.password, user.passwordHash);
    if (!valid) {
      throw new UnauthorizedException('用户名或密码错误');
    }

    const tokens = await this.generateTokens(user.id, user.username, user.email, user.role);
    return {
      ...tokens,
      user: { id: user.id, username: user.username, email: user.email, role: user.role, displayName: user.displayName },
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

      const tokens = await this.generateTokens(user.id, user.username, user.email, user.role);

      // Blacklist old tokenId AFTER new tokens are issued
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

  private async generateTokens(userId: string, username: string, email: string, role: string) {
    const tokenId = randomUUID();

    const accessSecret = this.configService.get<string>('jwt.accessSecret') as string;
    const accessExpires = this.configService.get<string>('jwt.accessExpiresIn') as string;
    const refreshSecret = this.configService.get<string>('jwt.refreshSecret') as string;
    const refreshExpires = this.configService.get<string>('jwt.refreshExpiresIn') as string;

    const [accessToken, refreshToken] = await Promise.all([
      this.jwtService.signAsync(
        { sub: userId, username, email, role },
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
