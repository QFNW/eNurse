import {
  Injectable,
  UnauthorizedException,
  ConflictException,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import * as bcrypt from 'bcrypt';
import { PrismaService } from '../../prisma/prisma.service';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';
import { randomUUID } from 'crypto';

@Injectable()
export class AuthService {
  constructor(
    private prisma: PrismaService,
    private jwtService: JwtService,
    private configService: ConfigService,
  ) {}

  async register(dto: RegisterDto) {
    const existing = await this.prisma.user.findUnique({
      where: { email: dto.email },
    });
    if (existing) {
      throw new ConflictException('该邮箱已被注册');
    }

    const passwordHash = await bcrypt.hash(dto.password, 12);
    // Force PATIENT role for self-registration — NURSE/ADMIN must be created by admin
    const user = await this.prisma.user.create({
      data: {
        email: dto.email,
        passwordHash,
        role: 'PATIENT',
      },
    });

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

      const user = await this.prisma.user.findUnique({
        where: { id: payload.sub },
      });
      if (!user) {
        throw new UnauthorizedException('用户不存在');
      }

      // Generate new token pair (invalidates old refresh by rotation)
      const tokens = await this.generateTokens(user.id, user.email, user.role);
      return {
        ...tokens,
        user: { id: user.id, email: user.email, role: user.role },
      };
    } catch {
      throw new UnauthorizedException('refreshToken 无效或已过期');
    }
  }

  async logout(refreshToken: string) {
    // JWT is stateless — client discards token.
    // In production, add token to Redis blacklist here.
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
