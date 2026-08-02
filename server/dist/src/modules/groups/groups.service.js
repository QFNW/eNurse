"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.GroupsService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../../prisma/prisma.service");
const crypto_1 = require("crypto");
const date_utils_1 = require("../../common/utils/date.utils");
let GroupsService = class GroupsService {
    prisma;
    constructor(prisma) {
        this.prisma = prisma;
    }
    async create(userId, dto) {
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
    async findByUser(userId, role) {
        if (role === 'NURSE') {
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
        const memberships = await this.prisma.groupMember.findMany({
            where: { userId },
            include: {
                group: {
                    select: {
                        id: true,
                        name: true,
                        inviteCode: true,
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
    async findById(userId, groupId) {
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
        if (!group)
            throw new common_1.NotFoundException('小组不存在');
        const memberEntry = group.members.find((m) => m.userId === userId);
        if (!memberEntry)
            throw new common_1.ForbiddenException('无权访问该小组');
        if (memberEntry.roleInGroup !== 'OWNER' && memberEntry.roleInGroup !== 'ADMIN') {
            const { inviteCode, ...rest } = group;
            return rest;
        }
        return group;
    }
    async update(userId, groupId, dto) {
        await this.ensureOwner(userId, groupId);
        return this.prisma.group.update({
            where: { id: groupId },
            data: { name: dto.name },
        });
    }
    async remove(userId, groupId) {
        await this.ensureOwner(userId, groupId);
        return this.prisma.group.delete({ where: { id: groupId } });
    }
    async regenerateInvite(userId, groupId) {
        await this.ensureOwner(userId, groupId);
        const inviteCode = await this.generateInviteCode();
        return this.prisma.group.update({
            where: { id: groupId },
            data: { inviteCode },
        });
    }
    async joinByInvite(userId, dto) {
        const user = await this.prisma.user.findUnique({ where: { id: userId } });
        if (!user)
            throw new common_1.NotFoundException('用户不存在');
        if (user.role !== 'PATIENT') {
            throw new common_1.ForbiddenException('只有患者可以加入小组');
        }
        const group = await this.prisma.group.findUnique({
            where: { inviteCode: dto.inviteCode },
        });
        if (!group)
            throw new common_1.BadRequestException('邀请码无效');
        return this.prisma.$transaction(async (tx) => {
            const existing = await tx.groupMember.findFirst({ where: { userId } });
            if (existing) {
                throw new common_1.BadRequestException('已加入其他小组，每个患者最多属于一个小组');
            }
            const alreadyMember = await tx.groupMember.findUnique({
                where: { groupId_userId: { groupId: group.id, userId } },
            });
            if (alreadyMember)
                throw new common_1.BadRequestException('已是该小组成员');
            return tx.groupMember.create({
                data: { groupId: group.id, userId, roleInGroup: 'MEMBER' },
                include: { group: true },
            });
        }, { isolationLevel: 'Serializable' });
    }
    async leave(userId, groupId) {
        const membership = await this.prisma.groupMember.findUnique({
            where: { groupId_userId: { groupId, userId } },
        });
        if (!membership)
            throw new common_1.NotFoundException('未加入该小组');
        if (membership.roleInGroup === 'OWNER') {
            throw new common_1.BadRequestException('组长不能退出，请先转让组长或删除小组');
        }
        return this.prisma.groupMember.delete({
            where: { groupId_userId: { groupId, userId } },
        });
    }
    async getMembers(userId, groupId) {
        const requester = await this.prisma.groupMember.findUnique({
            where: { groupId_userId: { groupId, userId } },
        });
        if (!requester)
            throw new common_1.ForbiddenException('只有小组成员可以查看成员列表');
        const isManager = requester.roleInGroup === 'OWNER' || requester.roleInGroup === 'ADMIN';
        const members = await this.prisma.groupMember.findMany({
            where: { groupId },
            include: {
                user: {
                    select: isManager
                        ? { id: true, email: true, nickname: true, displayName: true, role: true }
                        : { id: true, nickname: true, displayName: true, role: true },
                },
            },
        });
        if (!isManager) {
            return members.map((m) => ({
                id: m.id,
                groupId: m.groupId,
                userId: m.userId,
                roleInGroup: m.roleInGroup,
                joinedAt: m.joinedAt,
                user: m.user,
            }));
        }
        const memberIds = members.map((m) => m.userId);
        const alertCounts = await this.prisma.alert.groupBy({
            by: ['userId'],
            where: { userId: { in: memberIds }, isResolved: false },
            _count: { id: true },
        });
        const countMap = new Map(alertCounts.map((a) => [a.userId, a._count.id]));
        return members.map((m) => ({ ...m, alertCount: countMap.get(m.userId) ?? 0 }));
    }
    async removeMember(ownerId, groupId, memberId) {
        await this.ensureAdminOrOwner(ownerId, groupId);
        const membership = await this.prisma.groupMember.findUnique({
            where: { groupId_userId: { groupId, userId: memberId } },
        });
        if (!membership)
            throw new common_1.NotFoundException('成员不存在');
        if (membership.roleInGroup === 'OWNER') {
            throw new common_1.BadRequestException('不能移除小组创建者');
        }
        return this.prisma.groupMember.delete({
            where: { groupId_userId: { groupId, userId: memberId } },
        });
    }
    async getMemberAlerts(ownerId, groupId, memberId) {
        await this.ensureAdminOrOwner(ownerId, groupId);
        await this.ensureMemberInGroup(groupId, memberId);
        return this.prisma.alert.findMany({
            where: { userId: memberId },
            orderBy: { createdAt: 'desc' },
            take: 50,
        });
    }
    async getMemberEvents(ownerId, groupId, memberId, from, to) {
        await this.ensureAdminOrOwner(ownerId, groupId);
        await this.ensureMemberInGroup(groupId, memberId);
        const where = { userId: memberId };
        const dateFilter = {};
        if (from || to) {
            if (from)
                dateFilter.gte = (0, date_utils_1.parseDate)(from, 'from');
            if (to)
                dateFilter.lte = (0, date_utils_1.parseDate)(to, 'to');
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
    async getMemberStrategies(ownerId, groupId, memberId) {
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
    async updateMemberStrategies(ownerId, groupId, memberId, dto) {
        await this.ensureAdminOrOwner(ownerId, groupId);
        await this.ensureMemberInGroup(groupId, memberId);
        if (dto.intervalDays !== undefined || dto.reminderTime !== undefined || dto.isEnabled !== undefined) {
            const bmSettings = await this.prisma.bmReminderSetting.findUnique({
                where: { userId: memberId },
            });
            if (bmSettings?.isLocked) {
                throw new common_1.ForbiddenException('大便策略已锁定，请先解锁');
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
        if (dto.scheduleId) {
            const schedule = await this.prisma.medicationSchedule.findUnique({
                where: { id: dto.scheduleId },
                include: { medication: true },
            });
            if (!schedule || schedule.medication.userId !== memberId) {
                throw new common_1.NotFoundException('排班不存在');
            }
            if (schedule.isLocked) {
                throw new common_1.ForbiddenException('用药策略已锁定，请先解锁');
            }
            await this.prisma.medicationSchedule.update({
                where: { id: dto.scheduleId },
                data: {
                    ...(dto.scheduleType && { scheduleType: dto.scheduleType }),
                    ...(dto.intervalHours !== undefined && { intervalHours: dto.intervalHours }),
                    ...(dto.fixedTimes !== undefined && { fixedTimes: dto.fixedTimes }),
                    ...(dto.startTime !== undefined && { startTime: new Date(dto.startTime) }),
                    ...(dto.isEnabled !== undefined && { isEnabled: dto.isEnabled }),
                },
            });
        }
        return this.getMemberStrategies(ownerId, groupId, memberId);
    }
    async toggleLock(ownerId, groupId, memberId, target, scheduleId) {
        await this.ensureAdminOrOwner(ownerId, groupId);
        await this.ensureMemberInGroup(groupId, memberId);
        if (target === 'bm') {
            const current = await this.prisma.bmReminderSetting.findUnique({
                where: { userId: memberId },
            });
            if (!current) {
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
                throw new common_1.NotFoundException('排班不存在');
            }
            const updated = await this.prisma.medicationSchedule.update({
                where: { id: scheduleId },
                data: { isLocked: !current.isLocked },
            });
            return { target: 'medication_schedule', scheduleId, isLocked: updated.isLocked };
        }
        throw new common_1.BadRequestException('无效的目标类型');
    }
    async getMemberCalendar(ownerId, groupId, memberId, year, month, tzOffsetMinutes = 480) {
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
        const recordMap = new Map();
        for (const r of records) {
            const localTime = new Date(r.recordedAt.getTime() + tzOffsetMinutes * 60000);
            const day = localTime.toISOString().slice(0, 10);
            if (!recordMap.has(day))
                recordMap.set(day, []);
            recordMap.get(day).push(r);
        }
        const days = [];
        for (let d = 1; d <= daysInMonth; d++) {
            const date = `${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
            const dayRecords = recordMap.get(date) || [];
            days.push({ date, hasRecord: dayRecords.length > 0, records: dayRecords });
        }
        return { year, month, days };
    }
    async ensureOwner(userId, groupId) {
        const membership = await this.prisma.groupMember.findUnique({
            where: { groupId_userId: { groupId, userId } },
        });
        if (!membership || membership.roleInGroup !== 'OWNER') {
            throw new common_1.ForbiddenException('仅小组创建者可执行此操作');
        }
    }
    async ensureMemberInGroup(groupId, memberId) {
        const membership = await this.prisma.groupMember.findUnique({
            where: { groupId_userId: { groupId, userId: memberId } },
        });
        if (!membership)
            throw new common_1.NotFoundException('该用户不在此小组中');
    }
    async ensureAdminOrOwner(userId, groupId) {
        const membership = await this.prisma.groupMember.findUnique({
            where: { groupId_userId: { groupId, userId } },
        });
        if (!membership || (membership.roleInGroup !== 'OWNER' && membership.roleInGroup !== 'ADMIN')) {
            throw new common_1.ForbiddenException('仅小组管理员可执行此操作');
        }
    }
    async generateInviteCode() {
        const maxRetries = 5;
        for (let attempt = 0; attempt < maxRetries; attempt++) {
            const code = String((0, crypto_1.randomInt)(0, 1000000)).padStart(6, '0');
            const existing = await this.prisma.group.findUnique({
                where: { inviteCode: code },
            });
            if (!existing)
                return code;
        }
        return Date.now().toString().slice(-6);
    }
    async createMedicationForPatient(nurseId, groupId, patientId, dto) {
        const m = await this.prisma.groupMember.findUnique({ where: { groupId_userId: { groupId, userId: nurseId } } });
        if (!m)
            throw new common_1.ForbiddenException('无权操作');
        return this.prisma.medication.create({
            data: { userId: patientId, name: dto.name, dosage: dto.dosage ?? null },
        });
    }
    async joinAsStaff(userId, inviteCode) {
        const user = await this.prisma.user.findUnique({ where: { id: userId } });
        if (!user)
            throw new common_1.NotFoundException('用户不存在');
        if (user.role !== 'NURSE')
            throw new common_1.ForbiddenException('仅护理人员可申请');
        const group = await this.prisma.group.findUnique({ where: { inviteCode } });
        if (!group)
            throw new common_1.BadRequestException('邀请码无效');
        const alreadyMember = await this.prisma.groupMember.findUnique({ where: { groupId_userId: { groupId: group.id, userId } } });
        if (alreadyMember)
            throw new common_1.BadRequestException('已是该小组成员');
        const existingReq = await this.prisma.adminRequest.findUnique({ where: { groupId_userId: { groupId: group.id, userId } } });
        if (existingReq) {
            if (existingReq.status === 'PENDING')
                throw new common_1.BadRequestException('已有待审批申请');
            return this.prisma.adminRequest.update({ where: { id: existingReq.id }, data: { status: 'PENDING' } });
        }
        return this.prisma.adminRequest.create({ data: { groupId: group.id, userId, status: 'PENDING' } });
    }
    async getAdminRequests(userId, groupId) {
        await this.ensureAdminOrOwner(userId, groupId);
        return this.prisma.adminRequest.findMany({ where: { groupId, status: 'PENDING' }, include: { user: { select: { id: true, displayName: true, username: true, email: true } } } });
    }
    async approveAdminRequest(userId, groupId, requestId) {
        await this.ensureAdminOrOwner(userId, groupId);
        const req = await this.prisma.adminRequest.findUnique({ where: { id: requestId } });
        if (!req || req.status !== 'PENDING' || req.groupId !== groupId)
            throw new common_1.NotFoundException('申请不存在');
        await this.prisma.adminRequest.update({ where: { id: requestId }, data: { status: 'APPROVED' } });
        return this.prisma.groupMember.create({ data: { groupId, userId: req.userId, roleInGroup: 'ADMIN' } });
    }
    async rejectAdminRequest(userId, groupId, requestId) {
        await this.ensureAdminOrOwner(userId, groupId);
        return this.prisma.adminRequest.update({ where: { id: requestId }, data: { status: 'REJECTED' } });
    }
    async createVirtualPatient(nurseId, groupId, dto) {
        const m = await this.prisma.groupMember.findUnique({ where: { groupId_userId: { groupId, userId: nurseId } } });
        if (!m)
            throw new common_1.ForbiddenException('无权操作');
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
    async proxyBmRecord(nurseId, groupId, patientId, dto) {
        const m = await this.prisma.groupMember.findUnique({ where: { groupId_userId: { groupId, userId: nurseId } } });
        if (!m)
            throw new common_1.ForbiddenException('无权操作');
        return this.prisma.bmRecord.create({
            data: {
                userId: patientId,
                recordedAt: dto.recordedAt ? new Date(dto.recordedAt) : new Date(),
                notes: dto.notes ?? null,
            },
        });
    }
    async proxyMedicationRecord(nurseId, groupId, patientId, dto) {
        const m = await this.prisma.groupMember.findUnique({ where: { groupId_userId: { groupId, userId: nurseId } } });
        if (!m)
            throw new common_1.ForbiddenException('无权操作');
        return this.prisma.medicationRecord.create({
            data: {
                medicationId: dto.medicationId,
                userId: patientId,
                takenAt: new Date(),
                notes: dto.notes || null,
            },
        });
    }
};
exports.GroupsService = GroupsService;
exports.GroupsService = GroupsService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService])
], GroupsService);
//# sourceMappingURL=groups.service.js.map