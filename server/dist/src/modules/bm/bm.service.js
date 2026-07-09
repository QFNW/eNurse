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
exports.BmService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../../prisma/prisma.service");
const date_utils_1 = require("../../common/utils/date.utils");
let BmService = class BmService {
    prisma;
    constructor(prisma) {
        this.prisma = prisma;
    }
    async createRecord(userId, dto) {
        return this.prisma.bmRecord.create({
            data: {
                userId,
                recordedAt: new Date(),
                notes: dto.notes,
            },
        });
    }
    async getRecords(userId, query) {
        const page = Math.max(1, parseInt(query.page || '1', 10) || 1);
        const pageSize = Math.min(100, Math.max(1, parseInt(query.pageSize || '20', 10) || 20));
        const where = { userId };
        if (query.from || query.to) {
            where.recordedAt = {};
            if (query.from)
                where.recordedAt.gte = (0, date_utils_1.parseDate)(query.from, 'from');
            if (query.to)
                where.recordedAt.lte = (0, date_utils_1.parseDate)(query.to, 'to');
        }
        const [records, total] = await Promise.all([
            this.prisma.bmRecord.findMany({
                where,
                orderBy: { recordedAt: 'desc' },
                skip: (page - 1) * pageSize,
                take: pageSize,
            }),
            this.prisma.bmRecord.count({ where }),
        ]);
        return { records, total, page, pageSize };
    }
    async getRecordById(userId, id) {
        const record = await this.prisma.bmRecord.findUnique({ where: { id } });
        if (!record || record.userId !== userId) {
            throw new common_1.NotFoundException('记录不存在');
        }
        return record;
    }
    async updateRecord(userId, id, dto) {
        const record = await this.prisma.bmRecord.findUnique({ where: { id } });
        if (!record || record.userId !== userId) {
            throw new common_1.NotFoundException('记录不存在');
        }
        return this.prisma.bmRecord.update({
            where: { id },
            data: { notes: dto.notes },
        });
    }
    async deleteRecord(userId, id) {
        const record = await this.prisma.bmRecord.findUnique({ where: { id } });
        if (!record || record.userId !== userId) {
            throw new common_1.NotFoundException('记录不存在');
        }
        return this.prisma.bmRecord.delete({ where: { id } });
    }
    async getCalendar(userId, year, month, tzOffsetMinutes = 480) {
        const startDate = new Date(Date.UTC(year, month - 1, 1, -12, 0, 0, 0));
        const endDate = new Date(Date.UTC(year, month, 0, 23 + 12, 59, 59, 999));
        const records = await this.prisma.bmRecord.findMany({
            where: {
                userId,
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
    async getSettings(userId) {
        return this.prisma.bmReminderSetting.upsert({
            where: { userId },
            create: { userId },
            update: {},
        });
    }
    async updateSettings(userId, dto) {
        const settings = await this.getSettings(userId);
        if (settings.isLocked) {
            throw new common_1.ForbiddenException('策略已锁定，无法编辑');
        }
        return this.prisma.bmReminderSetting.update({
            where: { userId },
            data: {
                ...(dto.intervalDays !== undefined && { intervalDays: dto.intervalDays }),
                ...(dto.reminderTime !== undefined && { reminderTime: dto.reminderTime }),
                ...(dto.isEnabled !== undefined && { isEnabled: dto.isEnabled }),
            },
        });
    }
};
exports.BmService = BmService;
exports.BmService = BmService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService])
], BmService);
//# sourceMappingURL=bm.service.js.map