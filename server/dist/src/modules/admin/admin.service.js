"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.AdminService = void 0;
const common_1 = require("@nestjs/common");
const config_1 = require("@nestjs/config");
const jwt_1 = require("@nestjs/jwt");
const bcrypt = __importStar(require("bcrypt"));
const prisma_service_1 = require("../../prisma/prisma.service");
const VALID_ROLES = ['PATIENT', 'NURSE', 'ADMIN'];
let AdminService = class AdminService {
    prisma;
    jwtService;
    configService;
    constructor(prisma, jwtService, configService) {
        this.prisma = prisma;
        this.jwtService = jwtService;
        this.configService = configService;
    }
    async login(password) {
        const storedHash = this.configService.get('admin.passwordHash') ||
            process.env.ADMIN_PASSWORD_HASH;
        if (storedHash) {
            const valid = await bcrypt.compare(password, storedHash);
            if (!valid) {
                throw new common_1.UnauthorizedException('管理员密码错误');
            }
        }
        else {
            if (process.env.NODE_ENV === 'production') {
                throw new common_1.BadRequestException('服务配置错误：管理员密码未设置。请联系系统管理员配置 ADMIN_PASSWORD_HASH。');
            }
            const devPassword = process.env.ADMIN_PASSWORD || 'admin_enurse_2026';
            if (password !== devPassword) {
                throw new common_1.UnauthorizedException('管理员密码错误');
            }
        }
        const token = await this.jwtService.signAsync({ sub: 'admin', role: 'ADMIN' }, {
            secret: this.configService.get('jwt.accessSecret') ||
                'dev-access-secret',
            expiresIn: '2h',
        });
        return { accessToken: token };
    }
    async getUsers(search, page = 1, pageSize = 20) {
        const where = {};
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
    async updateUserRole(userId, role) {
        if (!VALID_ROLES.includes(role)) {
            throw new common_1.BadRequestException(`无效的角色: ${role}。有效值: ${VALID_ROLES.join(', ')}`);
        }
        const user = await this.prisma.user.findUnique({
            where: { id: userId },
        });
        if (!user)
            throw new common_1.NotFoundException('用户不存在');
        return this.prisma.user.update({
            where: { id: userId },
            data: { role: role },
            select: { id: true, email: true, nickname: true, role: true },
        });
    }
    async setUserStatus(userId, isDisabled, requesterId) {
        const user = await this.prisma.user.findUnique({
            where: { id: userId },
        });
        if (!user)
            throw new common_1.NotFoundException('用户不存在');
        if (user.role === 'ADMIN') {
            throw new common_1.BadRequestException('不能禁用管理员账号');
        }
        if (requesterId && userId === requesterId) {
            throw new common_1.BadRequestException('不能禁用自己的账号');
        }
        return this.prisma.user.update({
            where: { id: userId },
            data: { isDisabled },
            select: { id: true, email: true, nickname: true, role: true, isDisabled: true },
        });
    }
    async resetPassword(userId, newPassword) {
        const user = await this.prisma.user.findUnique({
            where: { id: userId },
        });
        if (!user)
            throw new common_1.NotFoundException('用户不存在');
        const passwordHash = await bcrypt.hash(newPassword, 12);
        await this.prisma.user.update({
            where: { id: userId },
            data: { passwordHash },
            select: { id: true, email: true },
        });
        return { message: '密码已重置' };
    }
    async deleteUser(userId) {
        const user = await this.prisma.user.findUnique({
            where: { id: userId },
        });
        if (!user)
            throw new common_1.NotFoundException('用户不存在');
        if (user.role === 'ADMIN') {
            throw new common_1.BadRequestException('不能注销管理员账号');
        }
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
    async getGroupMembers(groupId) {
        const group = await this.prisma.group.findUnique({
            where: { id: groupId },
            select: { id: true, name: true },
        });
        if (!group)
            throw new common_1.NotFoundException('小组不存在');
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
    async deleteGroup(groupId) {
        const group = await this.prisma.group.findUnique({
            where: { id: groupId },
            select: { id: true, name: true },
        });
        if (!group)
            throw new common_1.NotFoundException('小组不存在');
        await this.prisma.group.delete({ where: { id: groupId } });
        return { message: `小组「${group.name}」已解散` };
    }
    async getUserDetail(userId) {
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
        if (!user)
            throw new common_1.NotFoundException('用户不存在');
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
        const [totalUsers, totalPatients, totalNurses, totalGroups, totalBmRecords, totalMedicationRecords, activeAlerts, todayBmRecords, todayMedicationRecords,] = await Promise.all([
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
};
exports.AdminService = AdminService;
exports.AdminService = AdminService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService,
        jwt_1.JwtService,
        config_1.ConfigService])
], AdminService);
function utcToday() {
    const now = new Date();
    return new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()));
}
//# sourceMappingURL=admin.service.js.map