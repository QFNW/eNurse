import {
  Injectable,
  UnauthorizedException,
  NotFoundException,
  BadRequestException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { JwtService } from '@nestjs/jwt';
import * as bcrypt from 'bcrypt';
import { PrismaService } from '../../prisma/prisma.service';
import { Role } from '../../../generated/prisma/client';

const VALID_ROLES: Role[] = ['PATIENT', 'NURSE', 'ADMIN'];

@Injectable()
export class AdminService {
  constructor(
    private prisma: PrismaService,
    private jwtService: JwtService,
    private configService: ConfigService,
  ) {}

  async login(password: string) {
    const storedHash =
      this.configService.get<string>('admin.passwordHash') ||
      process.env.ADMIN_PASSWORD_HASH;

    if (storedHash) {
      const valid = await bcrypt.compare(password, storedHash);
      if (!valid) {
        throw new UnauthorizedException('管理员密码错误');
      }
    } else {
      // Dev-only plaintext fallback — refuse in production
      if (process.env.NODE_ENV === 'production') {
        throw new BadRequestException(
          '服务配置错误：管理员密码未设置。请联系系统管理员配置 ADMIN_PASSWORD_HASH。',
        );
      }
      const devPassword =
        process.env.ADMIN_PASSWORD || 'admin_enurse_2026';
      if (password !== devPassword) {
        throw new UnauthorizedException('管理员密码错误');
      }
    }

    const token = await this.jwtService.signAsync(
      { sub: 'admin', role: 'ADMIN' },
      {
        secret:
          this.configService.get<string>('jwt.accessSecret') ||
          'dev-access-secret',
        expiresIn: '2h',
      },
    );

    return { accessToken: token };
  }

  async getUsers(search?: string, page = 1, pageSize = 20) {
    const where: any = {};
    if (search) {
      where.OR = [
        { email: { contains: search } },
        { nickname: { contains: search } },
      ];
    }

    const [users, total] = await Promise.all([
      this.prisma.user.findMany({
        where,
        select: {
          id: true,
          email: true,
          nickname: true,
          role: true,
          wechatWxid: true,
          createdAt: true,
          _count: { select: { bmRecords: true, medications: true } },
        },
        orderBy: { createdAt: 'desc' },
        skip: (page - 1) * pageSize,
        take: pageSize,
      }),
      this.prisma.user.count({ where }),
    ]);

    return { users, total, page, pageSize };
  }

  async updateUserRole(userId: string, role: string) {
    if (!VALID_ROLES.includes(role as Role)) {
      throw new BadRequestException(
        `无效的角色: ${role}。有效值: ${VALID_ROLES.join(', ')}`,
      );
    }

    const user = await this.prisma.user.findUnique({
      where: { id: userId },
    });
    if (!user) throw new NotFoundException('用户不存在');

    return this.prisma.user.update({
      where: { id: userId },
      data: { role: role as Role },
      select: { id: true, email: true, nickname: true, role: true },
    });
  }

  async disableUser(userId: string, requesterId?: string) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
    });
    if (!user) throw new NotFoundException('用户不存在');
    if (user.role === 'ADMIN') {
      throw new BadRequestException('不能禁用管理员账号');
    }
    if (requesterId && userId === requesterId) {
      throw new BadRequestException('不能禁用自己的账号');
    }

    // Replace password with a random hash so user cannot log in
    const randomHash = await bcrypt.hash(
      `disabled_${userId}_${Date.now()}`,
      6,
    );

    return this.prisma.user.update({
      where: { id: userId },
      data: { passwordHash: randomHash },
      select: { id: true, email: true, role: true },
    });
  }

  async getGroups(page = 1, pageSize = 20) {
    const [groups, total] = await Promise.all([
      this.prisma.group.findMany({
        include: {
          _count: { select: { members: true } },
          creator: { select: { email: true, nickname: true } },
        },
        orderBy: { createdAt: 'desc' },
        skip: (page - 1) * pageSize,
        take: pageSize,
      }),
      this.prisma.group.count(),
    ]);

    return { groups, total, page, pageSize };
  }

  async getStats() {
    const [
      totalUsers,
      totalPatients,
      totalNurses,
      totalGroups,
      totalBmRecords,
      totalMedicationRecords,
      activeAlerts,
      todayBmRecords,
      todayMedicationRecords,
    ] = await Promise.all([
      this.prisma.user.count(),
      this.prisma.user.count({ where: { role: 'PATIENT' } }),
      this.prisma.user.count({ where: { role: 'NURSE' } }),
      this.prisma.group.count(),
      this.prisma.bmRecord.count(),
      this.prisma.medicationRecord.count(),
      this.prisma.alert.count({ where: { isResolved: false } }),
      this.prisma.bmRecord.count({
        where: {
          recordedAt: { gte: utcToday() },
        },
      }),
      this.prisma.medicationRecord.count({
        where: {
          takenAt: { gte: utcToday() },
        },
      }),
    ]);

    return {
      users: { total: totalUsers, patients: totalPatients, nurses: totalNurses },
      groups: totalGroups,
      records: {
        bmTotal: totalBmRecords,
        bmToday: todayBmRecords,
        medTotal: totalMedicationRecords,
        medToday: todayMedicationRecords,
      },
      alerts: { active: activeAlerts },
    };
  }
}

/** Returns UTC midnight (00:00:00.000Z) for today's date */
function utcToday(): Date {
  const now = new Date();
  return new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()));
}
