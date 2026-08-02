import {
  Injectable,
  NotFoundException,
  ForbiddenException,
  BadRequestException,
} from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';
import { parseDate } from '../../common/utils/date.utils';
import { Role } from '../../../generated/prisma/client';
import {
  CreateGroupDto,
  UpdateGroupDto,
  JoinGroupDto,
  UpdateMemberStrategyDto,
} from './dto/group.dto';

@Injectable()
export class GroupsService {
  constructor(private prisma: PrismaService) {}

  // ========== Group CRUD ==========

  async create(userId: string, dto: CreateGroupDto) {
    const inviteCode = await this.generateInviteCode();
    return this.prisma.group.create({
      data: {
        name: dto.name,
        inviteCode,
        createdBy: userId,
        members: {
          create: {
            userId,
            roleInGroup: 'OWNER',
          },
        },
      },
      include: { members: { include: { user: { select: { id: true, email: true, nickname: true, role: true } } } } },
    });
  }

  async findByUser(userId: string, role: string) {
    if (role === 'NURSE') {
      // Nurse sees groups they created OR where they're a member (e.g. as ADMIN)
      const memberGroupIds = await this.prisma.groupMember.findMany({
        where: { userId },
        select: { groupId: true },
      });
      const ids = memberGroupIds.map((m) => m.groupId);
      return this.prisma.group.findMany({
        where: {
          OR: [
            { createdBy: userId },
            { id: { in: ids } },
          ],
        },
        include: {
          _count: { select: { members: true } },
        },
        orderBy: { createdAt: 'desc' },
      });
    }
    // Patient sees groups they belong to (exclude inviteCode)
    const memberships = await this.prisma.groupMember.findMany({
      where: { userId },
      include: {
        group: {
          select: {
            id: true,
            name: true,
            createdBy: true,
            createdAt: true,
            updatedAt: true,
            creator: { select: { id: true, email: true, nickname: true, role: true } },
            _count: { select: { members: true } },
          },
        },
      },
    });
    return memberships.map((m) => m.group);
  }

  async findById(userId: string, groupId: string) {
    const group = await this.prisma.group.findUnique({
      where: { id: groupId },
      include: {
        _count: { select: { members: true } },
        members: {
          include: {
            user: {
              select: { id: true, email: true, nickname: true, role: true, displayName: true, username: true },
            },
          },
        },
      },
    });
    if (!group) throw new NotFoundException('小组不存在');

    // Verify user is a member
    const memberEntry = group.members.find((m) => m.userId === userId);
    if (!memberEntry) throw new ForbiddenException('无权访问该小组');

    // Exclude inviteCode for non-owner/non-admin
    if (memberEntry.roleInGroup !== 'OWNER' && memberEntry.roleInGroup !== 'ADMIN') {
      const { inviteCode, ...rest } = group;
      return rest;
    }

    return group;
  }

  async update(userId: string, groupId: string, dto: UpdateGroupDto) {
    await this.ensureOwner(userId, groupId);
    return this.prisma.group.update({
      where: { id: groupId },
      data: { name: dto.name },
    });
  }

  async remove(userId: string, groupId: string) {
    await this.ensureOwner(userId, groupId);
    return this.prisma.group.delete({ where: { id: groupId } });
  }

  async regenerateInvite(userId: string, groupId: string) {
    await this.ensureOwner(userId, groupId);
    const inviteCode = await this.generateInviteCode();
    return this.prisma.group.update({
      where: { id: groupId },
      data: { inviteCode },
    });
  }

  // ========== Join ==========

  async joinByInvite(userId: string, dto: JoinGroupDto) {
    // Check user role — only PATIENT can join
    const user = await this.prisma.user.findUnique({ where: { id: userId } });
    if (!user) throw new NotFoundException('用户不存在');
    if (user.role !== 'PATIENT') {
      throw new ForbiddenException('只有患者可以加入小组');
    }

    // Find group by invite code
    const group = await this.prisma.group.findUnique({
      where: { inviteCode: dto.inviteCode },
    });
    if (!group) throw new BadRequestException('邀请码无效');

    // Atomic check-and-create with serializable isolation to prevent concurrent joins
    return this.prisma.$transaction(
      async (tx) => {
        const existing = await tx.groupMember.findFirst({ where: { userId } });
        if (existing) {
          throw new BadRequestException('已加入其他小组，每个患者最多属于一个小组');
        }

        const alreadyMember = await tx.groupMember.findUnique({
          where: { groupId_userId: { groupId: group.id, userId } },
        });
        if (alreadyMember) throw new BadRequestException('已是该小组成员');

        return tx.groupMember.create({
          data: { groupId: group.id, userId, roleInGroup: 'MEMBER' },
          include: { group: true },
        });
      },
      { isolationLevel: 'Serializable' },
    );
  }

  // ========== Leave (Patient) ==========

  async leave(userId: string, groupId: string) {
    const membership = await this.prisma.groupMember.findUnique({
      where: { groupId_userId: { groupId, userId } },
    });
    if (!membership) throw new NotFoundException('未加入该小组');
    if (membership.roleInGroup === 'OWNER') {
      throw new BadRequestException('组长不能退出，请先转让组长或删除小组');
    }
    return this.prisma.groupMember.delete({
      where: { groupId_userId: { groupId, userId } },
    });
  }

  // ========== Members (Nurse) ==========

  async getMembers(userId: string, groupId: string) {
    await this.ensureAdminOrOwner(userId, groupId);
    const members = await this.prisma.groupMember.findMany({
      where: { groupId },
      include: {
        user: {
          select: { id: true, email: true, nickname: true, role: true },
        },
      },
    });

    // Batch alert counts in a single query
    const memberIds = members.map((m) => m.userId);
    const alertCounts = await this.prisma.alert.groupBy({
      by: ['userId'],
      where: { userId: { in: memberIds }, isResolved: false },
      _count: { id: true },
    });
    const countMap = new Map(
      alertCounts.map((a) => [a.userId, a._count.id]),
    );

    return members.map((m) => ({ ...m, alertCount: countMap.get(m.userId) ?? 0 }));
  }

  async removeMember(ownerId: string, groupId: string, memberId: string) {
    await this.ensureAdminOrOwner(ownerId, groupId);
    // Don't remove the owner
    const membership = await this.prisma.groupMember.findUnique({
      where: { groupId_userId: { groupId, userId: memberId } },
    });
    if (!membership) throw new NotFoundException('成员不存在');
    if (membership.roleInGroup === 'OWNER') {
      throw new BadRequestException('不能移除小组创建者');
    }

    return this.prisma.groupMember.delete({
      where: { groupId_userId: { groupId, userId: memberId } },
    });
  }

  // ========== Member detail views (Nurse) ==========

  async getMemberAlerts(ownerId: string, groupId: string, memberId: string) {
    await this.ensureAdminOrOwner(ownerId, groupId);
    await this.ensureMemberInGroup(groupId, memberId);

    return this.prisma.alert.findMany({
      where: { userId: memberId },
      orderBy: { createdAt: 'desc' },
      take: 50,
    });
  }

  async getMemberEvents(
    ownerId: string,
    groupId: string,
    memberId: string,
    from?: string,
    to?: string,
  ) {
    await this.ensureAdminOrOwner(ownerId, groupId);
    await this.ensureMemberInGroup(groupId, memberId);

    const where = { userId: memberId };
    const dateFilter: any = {};
    if (from || to) {
      if (from) dateFilter.gte = parseDate(from, 'from');
      if (to) dateFilter.lte = parseDate(to, 'to');
    }

    const [bmRecords, medRecords, alerts] = await Promise.all([
      this.prisma.bmRecord.findMany({
        where: { ...where, ...(Object.keys(dateFilter).length ? { recordedAt: dateFilter } : {}) },
        orderBy: { recordedAt: 'desc' },
        take: 50,
      }),
      this.prisma.medicationRecord.findMany({
        where: { ...where, ...(Object.keys(dateFilter).length ? { takenAt: dateFilter } : {}) },
        include: { medication: { select: { id: true, name: true } } },
        orderBy: { takenAt: 'desc' },
        take: 50,
      }),
      this.prisma.alert.findMany({
        where: {
          userId: memberId,
          ...(Object.keys(dateFilter).length ? { createdAt: dateFilter } : {}),
        },
        orderBy: { createdAt: 'desc' },
        take: 20,
      }),
    ]);

    return { bmRecords, medRecords, alerts };
  }

  async getMemberStrategies(
    ownerId: string,
    groupId: string,
    memberId: string,
  ) {
    await this.ensureAdminOrOwner(ownerId, groupId);
    await this.ensureMemberInGroup(groupId, memberId);

    const [bmSettings, medications] = await Promise.all([
      this.prisma.bmReminderSetting.findUnique({
        where: { userId: memberId },
      }),
      this.prisma.medication.findMany({
        where: { userId: memberId, isActive: true },
        include: { schedules: true },
      }),
    ]);

    return { bmSettings, medications };
  }

  async updateMemberStrategies(
    ownerId: string,
    groupId: string,
    memberId: string,
    dto: UpdateMemberStrategyDto,
  ) {
    await this.ensureAdminOrOwner(ownerId, groupId);
    await this.ensureMemberInGroup(groupId, memberId);

    // Update BM settings
    if (dto.intervalDays !== undefined || dto.reminderTime !== undefined || dto.isEnabled !== undefined) {
      const bmSettings = await this.prisma.bmReminderSetting.findUnique({
        where: { userId: memberId },
      });
      if (bmSettings?.isLocked) {
        throw new ForbiddenException('大便策略已锁定，请先解锁');
      }

      await this.prisma.bmReminderSetting.upsert({
        where: { userId: memberId },
        create: {
          userId: memberId,
          intervalDays: dto.intervalDays ?? 1,
          reminderTime: dto.reminderTime ?? '08:00',
          isEnabled: dto.isEnabled ?? true,
        },
        update: {
          ...(dto.intervalDays !== undefined && { intervalDays: dto.intervalDays }),
          ...(dto.reminderTime !== undefined && { reminderTime: dto.reminderTime }),
          ...(dto.isEnabled !== undefined && { isEnabled: dto.isEnabled }),
        },
      });
    }

    // Update medication schedule
    if (dto.scheduleId) {
      const schedule = await this.prisma.medicationSchedule.findUnique({
        where: { id: dto.scheduleId },
        include: { medication: true },
      });
      if (!schedule || schedule.medication.userId !== memberId) {
        throw new NotFoundException('排班不存在');
      }
      if (schedule.isLocked) {
        throw new ForbiddenException('用药策略已锁定，请先解锁');
      }

      await this.prisma.medicationSchedule.update({
        where: { id: dto.scheduleId },
        data: {
          ...(dto.scheduleType && { scheduleType: dto.scheduleType as any }),
          ...(dto.intervalHours !== undefined && { intervalHours: dto.intervalHours }),
          ...(dto.fixedTimes !== undefined && { fixedTimes: dto.fixedTimes as any }),
          ...(dto.startTime !== undefined && { startTime: new Date(dto.startTime) }),
          ...(dto.isEnabled !== undefined && { isEnabled: dto.isEnabled }),
        },
      });
    }

    return this.getMemberStrategies(ownerId, groupId, memberId);
  }

  async toggleLock(
    ownerId: string,
    groupId: string,
    memberId: string,
    target: 'bm' | 'medication_schedule',
    scheduleId?: string,
  ) {
    await this.ensureAdminOrOwner(ownerId, groupId);
    await this.ensureMemberInGroup(groupId, memberId);

    if (target === 'bm') {
      const current = await this.prisma.bmReminderSetting.findUnique({
        where: { userId: memberId },
      });
      if (!current) {
        // Create default settings first
        await this.prisma.bmReminderSetting.create({ data: { userId: memberId, isLocked: true } });
        return { target: 'bm', isLocked: true };
      }
      const updated = await this.prisma.bmReminderSetting.update({
        where: { userId: memberId },
        data: { isLocked: !current.isLocked },
      });
      return { target: 'bm', isLocked: updated.isLocked };
    }

    if (target === 'medication_schedule' && scheduleId) {
      const current = await this.prisma.medicationSchedule.findUnique({
        where: { id: scheduleId },
        include: { medication: true },
      });
      if (!current || current.medication.userId !== memberId) {
        throw new NotFoundException('排班不存在');
      }
      const updated = await this.prisma.medicationSchedule.update({
        where: { id: scheduleId },
        data: { isLocked: !current.isLocked },
      });
      return { target: 'medication_schedule', scheduleId, isLocked: updated.isLocked };
    }

    throw new BadRequestException('无效的目标类型');
  }

  async getMemberCalendar(
    ownerId: string,
    groupId: string,
    memberId: string,
    year: number,
    month: number,
    tzOffsetMinutes = 480,
  ) {
    await this.ensureAdminOrOwner(ownerId, groupId);
    await this.ensureMemberInGroup(groupId, memberId);

    const startDate = new Date(Date.UTC(year, month - 1, 1, -12, 0, 0, 0));
    const endDate = new Date(Date.UTC(year, month, 0, 23 + 12, 59, 59, 999));

    const records = await this.prisma.bmRecord.findMany({
      where: {
        userId: memberId,
        recordedAt: { gte: startDate, lte: endDate },
      },
      orderBy: { recordedAt: 'asc' },
    });

    const daysInMonth = new Date(year, month, 0).getDate();
    const recordMap = new Map<string, typeof records>();
    for (const r of records) {
      const localTime = new Date(r.recordedAt.getTime() + tzOffsetMinutes * 60000);
      const day = localTime.toISOString().slice(0, 10);
      if (!recordMap.has(day)) recordMap.set(day, []);
      recordMap.get(day)!.push(r);
    }

    const days = [];
    for (let d = 1; d <= daysInMonth; d++) {
      const date = `${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
      const dayRecords = recordMap.get(date) || [];
      days.push({ date, hasRecord: dayRecords.length > 0, records: dayRecords });
    }

    return { year, month, days };
  }

  // ========== Helpers ==========

  private async ensureOwner(userId: string, groupId: string) {
    const membership = await this.prisma.groupMember.findUnique({
      where: { groupId_userId: { groupId, userId } },
    });
    if (!membership || membership.roleInGroup !== 'OWNER') {
      throw new ForbiddenException('仅小组创建者可执行此操作');
    }
  }

  private async ensureMemberInGroup(groupId: string, memberId: string) {
    const membership = await this.prisma.groupMember.findUnique({
      where: { groupId_userId: { groupId, userId: memberId } },
    });
    if (!membership) throw new NotFoundException('该用户不在此小组中');
  }

  private async ensureAdminOrOwner(userId: string, groupId: string) {
    const membership = await this.prisma.groupMember.findUnique({
      where: { groupId_userId: { groupId, userId } },
    });
    if (!membership || (membership.roleInGroup !== 'OWNER' && membership.roleInGroup !== 'ADMIN')) {
      throw new ForbiddenException('仅小组管理员可执行此操作');
    }
  }

  private async generateInviteCode(): Promise<string> {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    const maxRetries = 5;

    for (let attempt = 0; attempt < maxRetries; attempt++) {
      let code = '';
      for (let i = 0; i < 8; i++) {
        code += chars[Math.floor(Math.random() * chars.length)];
      }
      const existing = await this.prisma.group.findUnique({
        where: { inviteCode: code },
      });
      if (!existing) return code;
    }

    // Fallback: generate random codes with DB check
    for (let attempt = 0; attempt < 5; attempt++) {
      let code = '';
      for (let i = 0; i < 8; i++) {
        code += chars[Math.floor(Math.random() * chars.length)];
      }
      const existing = await this.prisma.group.findUnique({
        where: { inviteCode: code },
      });
      if (!existing) return code;
    }
    // Last resort: crypto-random hex + DB check
    const crypto = await import('crypto');
    const hex = crypto.randomBytes(6).toString('hex').toUpperCase().slice(0, 8);
    const existing = await this.prisma.group.findUnique({
      where: { inviteCode: hex },
    });
    if (!existing) return hex;
    // Ultra-fallback: timestamp suffix
    const ts = Date.now().toString(36).toUpperCase();
    return ts.slice(-8).padStart(8, 'X');
  }

  async createMedicationForPatient(nurseId: string, groupId: string, patientId: string, dto: { name: string; dosage?: string }) {
    const m = await this.prisma.groupMember.findUnique({ where: { groupId_userId: { groupId, userId: nurseId } } });
    if (!m) throw new ForbiddenException('无权操作');
    return this.prisma.medication.create({
      data: { userId: patientId, name: dto.name, dosage: dto.dosage ?? null },
    });
  }

  async joinAsStaff(userId: string, inviteCode: string) {
    const user = await this.prisma.user.findUnique({ where: { id: userId } });
    if (!user) throw new NotFoundException('用户不存在');
    if (user.role !== 'NURSE') throw new ForbiddenException('仅护理人员可申请');

    const group = await this.prisma.group.findUnique({ where: { inviteCode } });
    if (!group) throw new BadRequestException('邀请码无效');

    const alreadyMember = await this.prisma.groupMember.findUnique({ where: { groupId_userId: { groupId: group.id, userId } } });
    if (alreadyMember) throw new BadRequestException('已是该小组成员');

    const existingReq = await this.prisma.adminRequest.findUnique({ where: { groupId_userId: { groupId: group.id, userId } } });
    if (existingReq) {
      if (existingReq.status === 'PENDING') throw new BadRequestException('已有待审批申请');
      // APPROVED or REJECTED: allow re-apply — update existing record back to PENDING
      return this.prisma.adminRequest.update({ where: { id: existingReq.id }, data: { status: 'PENDING' } });
    }

    return this.prisma.adminRequest.create({ data: { groupId: group.id, userId, status: 'PENDING' } });
  }

  async getAdminRequests(userId: string, groupId: string) {
    await this.ensureAdminOrOwner(userId, groupId);
    return this.prisma.adminRequest.findMany({ where: { groupId, status: 'PENDING' }, include: { user: { select: { id: true, displayName: true, username: true, email: true } } } });
  }

  async approveAdminRequest(userId: string, groupId: string, requestId: string) {
    await this.ensureAdminOrOwner(userId, groupId);
    const req = await this.prisma.adminRequest.findUnique({ where: { id: requestId } });
    if (!req || req.status !== 'PENDING' || req.groupId !== groupId) throw new NotFoundException('申请不存在');

    await this.prisma.adminRequest.update({ where: { id: requestId }, data: { status: 'APPROVED' } });
    return this.prisma.groupMember.create({ data: { groupId, userId: req.userId, roleInGroup: 'ADMIN' } });
  }

  async rejectAdminRequest(userId: string, groupId: string, requestId: string) {
    await this.ensureAdminOrOwner(userId, groupId);
    return this.prisma.adminRequest.update({ where: { id: requestId }, data: { status: 'REJECTED' } });
  }

  // ========== 虚拟患者 ==========

  async createVirtualPatient(nurseId: string, groupId: string, dto: { name: string; notes?: string }) {
    const m = await this.prisma.groupMember.findUnique({ where: { groupId_userId: { groupId, userId: nurseId } } });
    if (!m) throw new ForbiddenException('无权操作');

    // Generate unique username for virtual patient
    const vId = `virt_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`;
    const virtualUser = await this.prisma.user.create({
      data: {
        username: vId,
        email: `${vId}@virtual.local`,
        passwordHash: '',
        displayName: dto.name,
        role: 'PATIENT',
        isVirtual: true,
      },
    });

    await this.prisma.groupMember.create({
      data: { groupId, userId: virtualUser.id, roleInGroup: 'MEMBER' },
    });

    return virtualUser;
  }

  // ========== 护理端代打卡 ==========

  async proxyBmRecord(nurseId: string, groupId: string, patientId: string, dto: { notes?: string; recordedAt?: string }) {
    const m = await this.prisma.groupMember.findUnique({ where: { groupId_userId: { groupId, userId: nurseId } } });
    if (!m) throw new ForbiddenException('无权操作');
    return this.prisma.bmRecord.create({
      data: {
        userId: patientId,
        recordedAt: dto.recordedAt ? new Date(dto.recordedAt) : new Date(),
        notes: dto.notes ?? null,
      },
    });
  }

  async proxyMedicationRecord(nurseId: string, groupId: string, patientId: string, dto: { medicationId: string; notes?: string }) {
    const m = await this.prisma.groupMember.findUnique({ where: { groupId_userId: { groupId, userId: nurseId } } });
    if (!m) throw new ForbiddenException('无权操作');
    return this.prisma.medicationRecord.create({
      data: {
        medicationId: dto.medicationId,
        userId: patientId,
        takenAt: new Date(),
        notes: dto.notes || null,
      },
    });
  }
}
