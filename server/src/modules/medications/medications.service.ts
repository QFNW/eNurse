import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';
import {
  CreateMedicationDto,
  UpdateMedicationDto,
  CreateScheduleDto,
  UpdateScheduleDto,
  CreateMedicationRecordDto,
} from './dto/medication.dto';

@Injectable()
export class MedicationsService {
  constructor(private prisma: PrismaService) {}

  // ========== Medications ==========

  async create(userId: string, dto: CreateMedicationDto) {
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

  async findAll(userId: string) {
    return this.prisma.medication.findMany({
      where: { userId, isActive: true },
      include: { schedules: true },
      orderBy: { createdAt: 'desc' },
    });
  }

  async findById(userId: string, id: string) {
    const med = await this.prisma.medication.findUnique({
      where: { id },
      include: { schedules: true },
    });
    if (!med || med.userId !== userId) {
      throw new NotFoundException('药品不存在');
    }
    return med;
  }

  async update(userId: string, id: string, dto: UpdateMedicationDto) {
    const med = await this.prisma.medication.findUnique({ where: { id } });
    if (!med || med.userId !== userId) {
      throw new NotFoundException('药品不存在');
    }
    return this.prisma.medication.update({
      where: { id },
      data: { ...dto },
    });
  }

  async remove(userId: string, id: string) {
    const med = await this.prisma.medication.findUnique({ where: { id } });
    if (!med || med.userId !== userId) {
      throw new NotFoundException('药品不存在');
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

  // ========== Schedules ==========

  async createSchedule(
    userId: string,
    medicationId: string,
    dto: CreateScheduleDto,
  ) {
    const med = await this.prisma.medication.findUnique({
      where: { id: medicationId },
    });
    if (!med || med.userId !== userId) {
      throw new NotFoundException('药品不存在');
    }
    if (!med.isActive) {
      throw new NotFoundException('药品已删除，无法添加排班');
    }

    return this.prisma.medicationSchedule.create({
      data: {
        medicationId,
        scheduleType: dto.scheduleType,
        intervalHours: dto.intervalHours,
        fixedTimes: dto.fixedTimes as any,
        startTime: dto.startTime ? new Date(dto.startTime) : null,
      },
    });
  }

  async updateSchedule(
    userId: string,
    scheduleId: string,
    dto: UpdateScheduleDto,
  ) {
    const schedule = await this.prisma.medicationSchedule.findUnique({
      where: { id: scheduleId },
      include: { medication: true },
    });
    if (!schedule || schedule.medication.userId !== userId) {
      throw new NotFoundException('排班不存在');
    }
    if (schedule.isLocked) {
      throw new ForbiddenException('策略已锁定，无法编辑');
    }

    return this.prisma.medicationSchedule.update({
      where: { id: scheduleId },
      data: {
        ...(dto.intervalHours !== undefined && { intervalHours: dto.intervalHours }),
        ...(dto.fixedTimes !== undefined && { fixedTimes: dto.fixedTimes as any }),
        ...(dto.startTime !== undefined && { startTime: new Date(dto.startTime) }),
        ...(dto.isEnabled !== undefined && { isEnabled: dto.isEnabled }),
      },
    });
  }

  async deleteSchedule(userId: string, scheduleId: string) {
    const schedule = await this.prisma.medicationSchedule.findUnique({
      where: { id: scheduleId },
      include: { medication: true },
    });
    if (!schedule || schedule.medication.userId !== userId) {
      throw new NotFoundException('排班不存在');
    }
    if (schedule.isLocked) {
      throw new ForbiddenException('策略已锁定，无法删除');
    }
    return this.prisma.medicationSchedule.delete({
      where: { id: scheduleId },
    });
  }

  // ========== Records ==========

  async createRecord(userId: string, dto: CreateMedicationRecordDto) {
    const med = await this.prisma.medication.findUnique({
      where: { id: dto.medicationId },
    });
    if (!med || med.userId !== userId) {
      throw new NotFoundException('药品不存在');
    }
    return this.prisma.medicationRecord.create({
      data: {
        medicationId: dto.medicationId,
        userId,
        takenAt: new Date(dto.takenAt),
      },
    });
  }

  async getRecords(userId: string, from?: string, to?: string) {
    const where: any = { userId };
    if (from || to) {
      where.takenAt = {};
      if (from) where.takenAt.gte = new Date(from);
      if (to) where.takenAt.lte = new Date(to);
    }
    return this.prisma.medicationRecord.findMany({
      where,
      include: { medication: { select: { id: true, name: true } } },
      orderBy: { takenAt: 'desc' },
      take: 100,
    });
  }
}
