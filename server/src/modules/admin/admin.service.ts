import {
  Injectable,
  UnauthorizedException,
  NotFoundException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { JwtService } from '@nestjs/jwt';
import { PrismaService } from '../../prisma/prisma.service';

@Injectable()
export class AdminService {
  constructor(
    private prisma: PrismaService,
    private jwtService: JwtService,
    private configService: ConfigService,
  ) {}

  async login(password: string) {
    const adminPassword = this.configService.get<string>('admin.password') ||
      process.env.ADMIN_PASSWORD ||
      'admin_enurse_2026';

    if (password !== adminPassword) {
      throw new UnauthorizedException('管理员密码错误');
    }

    const token = await this.jwtService.signAsync(
      { role: 'ADMIN' },
      {
        secret: this.configService.get<string>('jwt.accessSecret') || 'dev-access-secret',
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
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
    });
    if (!user) throw new NotFoundException('用户不存在');

    return this.prisma.user.update({
      where: { id: userId },
      data: { role: role as any },
      select: { id: true, email: true, nickname: true, role: true },
    });
  }

  async disableUser(userId: string) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
    });
    if (!user) throw new NotFoundException('用户不存在');

    // Soft disable by clearing password (forces password reset)
    // Alternative: add an isDisabled field to User model
    return this.prisma.user.update({
      where: { id: userId },
      data: { role: 'PATIENT' }, // downgrade to patient on disable
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
          recordedAt: { gte: new Date(new Date().setHours(0, 0, 0, 0)) },
        },
      }),
      this.prisma.medicationRecord.count({
        where: {
          takenAt: { gte: new Date(new Date().setHours(0, 0, 0, 0)) },
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
