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
          isDisabled: true,
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

  async setUserStatus(userId: string, isDisabled: boolean, requesterId?: string) {
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

    return this.prisma.user.update({
      where: { id: userId },
      data: { isDisabled },
      select: { id: true, email: true, nickname: true, role: true, isDisabled: true },
    });
  }

  async resetPassword(userId: string, newPassword: string) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
    });
    if (!user) throw new NotFoundException('用户不存在');

    const passwordHash = await bcrypt.hash(newPassword, 12);
    await this.prisma.user.update({
      where: { id: userId },
      data: { passwordHash },
      select: { id: true, email: true },
    });
    return { message: '密码已重置' };
  }

  async deleteUser(userId: string) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
    });
    if (!user) throw new NotFoundException('用户不存在');
    if (user.role === 'ADMIN') {
      throw new BadRequestException('不能注销管理员账号');
    }

    // Delete groups this user owns, otherwise they'd be orphaned without an owner
    const ownedGroups = await this.prisma.groupMember.findMany({
      where: { userId, roleInGroup: 'OWNER' },
      select: { groupId: true },
    });
    if (ownedGroups.length > 0) {
      await this.prisma.group.deleteMany({
        where: { id: { in: ownedGroups.map((m) => m.groupId) } },
      });
    }

    await this.prisma.user.delete({ where: { id: userId } });
    return { message: '用户已注销' };
  }

  async getGroupMembers(groupId: string) {
    const group = await this.prisma.group.findUnique({
      where: { id: groupId },
      select: { id: true, name: true },
    });
    if (!group) throw new NotFoundException('小组不存在');

    const members = await this.prisma.groupMember.findMany({
      where: { groupId },
      select: {
        id: true,
        roleInGroup: true,
        joinedAt: true,
        user: {
          select: { id: true, email: true, nickname: true, displayName: true, role: true },
        },
      },
      orderBy: { joinedAt: 'asc' },
    });

    return {
      group,
      members: members.map((m) => ({
        ...m,
        user: m.user
          ? {
              ...m.user,
              displayName: m.user.displayName ?? m.user.nickname ?? '用户',
            }
          : null,
      })),
    };
  }

  async deleteGroup(groupId: string) {
    const group = await this.prisma.group.findUnique({
      where: { id: groupId },
      select: { id: true, name: true },
    });
    if (!group) throw new NotFoundException('小组不存在');

    await this.prisma.group.delete({ where: { id: groupId } });
    return { message: `小组「${group.name}」已解散` };
  }

  async getUserDetail(userId: string) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: {
        id: true,
        username: true,
        email: true,
        displayName: true,
        nickname: true,
        role: true,
        isVirtual: true,
        isDisabled: true,
        createdAt: true,
        updatedAt: true,
        _count: {
          select: { bmRecords: true, medicationRecords: true, groupMemberships: true },
        },
      },
    });
    if (!user) throw new NotFoundException('用户不存在');

    const [groups, bmRecords, medicationRecords, medications] = await Promise.all([
      this.prisma.groupMember.findMany({
        where: { userId },
        select: {
          roleInGroup: true,
          joinedAt: true,
          group: {
            select: {
              id: true,
              name: true,
              _count: { select: { members: true } },
            },
          },
        },
        orderBy: { joinedAt: 'desc' },
      }),
      this.prisma.bmRecord.findMany({
        where: { userId },
        orderBy: { recordedAt: 'desc' },
        take: 20,
      }),
      this.prisma.medicationRecord.findMany({
        where: { userId },
        include: { medication: { select: { id: true, name: true } } },
        orderBy: { takenAt: 'desc' },
        take: 20,
      }),
      this.prisma.medication.findMany({
        where: { userId, isActive: true },
        orderBy: { createdAt: 'desc' },
      }),
    ]);

    return {
      ...user,
      groups: groups.map((m) => ({
        id: m.group.id,
        name: m.group.name,
        roleInGroup: m.roleInGroup,
        joinedAt: m.joinedAt,
        memberCount: m.group._count.members,
      })),
      bmRecords,
      medicationRecords,
      medications,
      counts: user._count,
    };
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

  async getFeedback(page = 1, pageSize = 20) {
    const [feedbacks, total] = await Promise.all([
      this.prisma.feedback.findMany({
        include: {
          user: { select: { email: true, nickname: true, displayName: true } },
        },
        orderBy: { createdAt: 'desc' },
        skip: (page - 1) * pageSize,
        take: pageSize,
      }),
      this.prisma.feedback.count(),
    ]);

    return { feedbacks, total, page, pageSize };
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
