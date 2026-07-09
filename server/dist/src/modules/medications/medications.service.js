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
exports.MedicationsService = void 0;
const common_1 = require("@nestjs/common");
const date_utils_1 = require("../../common/utils/date.utils");
const prisma_service_1 = require("../../prisma/prisma.service");
let MedicationsService = class MedicationsService {
    prisma;
    constructor(prisma) {
        this.prisma = prisma;
    }
    async create(userId, dto) {
        return this.prisma.medication.create({
            data: {
                userId,
                name: dto.name,
                dosage: dto.dosage,
                unit: dto.unit,
                notes: dto.notes,
            },
        });
    }
    async findAll(userId) {
        return this.prisma.medication.findMany({
            where: { userId, isActive: true },
            include: { schedules: true },
            orderBy: { createdAt: 'desc' },
        });
    }
    async findById(userId, id) {
        const med = await this.prisma.medication.findUnique({
            where: { id },
            include: { schedules: true },
        });
        if (!med || med.userId !== userId) {
            throw new common_1.NotFoundException('药品不存在');
        }
        return med;
    }
    async update(userId, id, dto) {
        const med = await this.prisma.medication.findUnique({ where: { id } });
        if (!med || med.userId !== userId) {
            throw new common_1.NotFoundException('药品不存在');
        }
        const { isActive: _, ...safeData } = dto;
        return this.prisma.medication.update({
            where: { id },
            data: safeData,
        });
    }
    async remove(userId, id) {
        const med = await this.prisma.medication.findUnique({ where: { id } });
        if (!med || med.userId !== userId) {
            throw new common_1.NotFoundException('药品不存在');
        }
        return this.prisma.$transaction(async (tx) => {
            await tx.medicationSchedule.updateMany({
                where: { medicationId: id },
                data: { isEnabled: false },
            });
            return tx.medication.update({
                where: { id },
                data: { isActive: false },
            });
        });
    }
    async createSchedule(userId, medicationId, dto) {
        const med = await this.prisma.medication.findUnique({
            where: { id: medicationId },
        });
        if (!med || med.userId !== userId) {
            throw new common_1.NotFoundException('药品不存在');
        }
        if (!med.isActive) {
            throw new common_1.NotFoundException('药品已删除，无法添加排班');
        }
        return this.prisma.medicationSchedule.create({
            data: {
                medicationId,
                scheduleType: dto.scheduleType,
                intervalHours: dto.intervalHours,
                fixedTimes: dto.fixedTimes,
                startTime: dto.startTime ? new Date(dto.startTime) : new Date(),
            },
        });
    }
    async updateSchedule(userId, scheduleId, dto) {
        const schedule = await this.prisma.medicationSchedule.findUnique({
            where: { id: scheduleId },
            include: { medication: true },
        });
        if (!schedule || schedule.medication.userId !== userId) {
            throw new common_1.NotFoundException('排班不存在');
        }
        if (schedule.isLocked) {
            throw new common_1.ForbiddenException('策略已锁定，无法编辑');
        }
        return this.prisma.medicationSchedule.update({
            where: { id: scheduleId },
            data: {
                ...(dto.intervalHours !== undefined && { intervalHours: dto.intervalHours }),
                ...(dto.fixedTimes !== undefined && { fixedTimes: dto.fixedTimes }),
                ...(dto.startTime !== undefined && { startTime: new Date(dto.startTime) }),
                ...(dto.isEnabled !== undefined && { isEnabled: dto.isEnabled }),
            },
        });
    }
    async deleteSchedule(userId, scheduleId) {
        const schedule = await this.prisma.medicationSchedule.findUnique({
            where: { id: scheduleId },
            include: { medication: true },
        });
        if (!schedule || schedule.medication.userId !== userId) {
            throw new common_1.NotFoundException('排班不存在');
        }
        if (schedule.isLocked) {
            throw new common_1.ForbiddenException('策略已锁定，无法删除');
        }
        return this.prisma.medicationSchedule.delete({
            where: { id: scheduleId },
        });
    }
    async createRecord(userId, dto) {
        const med = await this.prisma.medication.findUnique({
            where: { id: dto.medicationId },
        });
        if (!med || med.userId !== userId) {
            throw new common_1.NotFoundException('药品不存在');
        }
        return this.prisma.medicationRecord.create({
            data: {
                medicationId: dto.medicationId,
                userId,
                takenAt: new Date(),
                ...(dto.notes ? { notes: dto.notes } : {}),
            },
        });
    }
    async getRecords(userId, from, to) {
        const where = { userId };
        if (from || to) {
            where.takenAt = {};
            if (from)
                where.takenAt.gte = (0, date_utils_1.parseDate)(from, 'from');
            if (to)
                where.takenAt.lte = (0, date_utils_1.parseDate)(to, 'to');
        }
        return this.prisma.medicationRecord.findMany({
            where,
            include: { medication: { select: { id: true, name: true } } },
            orderBy: { takenAt: 'desc' },
            take: 100,
        });
    }
};
exports.MedicationsService = MedicationsService;
exports.MedicationsService = MedicationsService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService])
], MedicationsService);
//# sourceMappingURL=medications.service.js.map