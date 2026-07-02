import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';
import {
  CreateBmRecordDto,
  QueryBmRecordsDto,
  UpdateBmRecordDto,
  UpdateBmSettingsDto,
} from './dto/bm.dto';

@Injectable()
export class BmService {
  constructor(private prisma: PrismaService) {}

  // ========== Records ==========

  async createRecord(userId: string, dto: CreateBmRecordDto) {
    return this.prisma.bmRecord.create({
      data: {
        userId,
        recordedAt: new Date(dto.recordedAt),
        notes: dto.notes,
      },
    });
  }

  async getRecords(userId: string, query: QueryBmRecordsDto) {
    const page = Math.max(1, parseInt(query.page || '1', 10) || 1);
    const pageSize = Math.min(100, Math.max(1, parseInt(query.pageSize || '20', 10) || 20));
    const where: any = { userId };

    if (query.from || query.to) {
      where.recordedAt = {};
      if (query.from) where.recordedAt.gte = new Date(query.from);
      if (query.to) where.recordedAt.lte = new Date(query.to);
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

  async getRecordById(userId: string, id: string) {
    const record = await this.prisma.bmRecord.findUnique({ where: { id } });
    if (!record || record.userId !== userId) {
      throw new NotFoundException('记录不存在');
    }
    return record;
  }

  async updateRecord(userId: string, id: string, dto: UpdateBmRecordDto) {
    const record = await this.prisma.bmRecord.findUnique({ where: { id } });
    if (!record || record.userId !== userId) {
      throw new NotFoundException('记录不存在');
    }
    return this.prisma.bmRecord.update({
      where: { id },
      data: { notes: dto.notes },
    });
  }

  async deleteRecord(userId: string, id: string) {
    const record = await this.prisma.bmRecord.findUnique({ where: { id } });
    if (!record || record.userId !== userId) {
      throw new NotFoundException('记录不存在');
    }
    return this.prisma.bmRecord.delete({ where: { id } });
  }

  // ========== Calendar ==========

  async getCalendar(userId: string, year: number, month: number) {
    const startDate = new Date(Date.UTC(year, month - 1, 1));
    const endDate = new Date(Date.UTC(year, month, 0, 23, 59, 59, 999));

    const records = await this.prisma.bmRecord.findMany({
      where: {
        userId,
        recordedAt: { gte: startDate, lte: endDate },
      },
      orderBy: { recordedAt: 'asc' },
    });

    // Build day-by-day map
    const daysInMonth = new Date(year, month, 0).getDate();
    const recordMap = new Map<string, typeof records>();
    for (const r of records) {
      const day = r.recordedAt.toISOString().slice(0, 10);
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

  // ========== Settings ==========

  async getSettings(userId: string) {
    let settings = await this.prisma.bmReminderSetting.findUnique({
      where: { userId },
    });
    if (!settings) {
      settings = await this.prisma.bmReminderSetting.create({
        data: { userId },
      });
    }
    return settings;
  }

  async updateSettings(userId: string, dto: UpdateBmSettingsDto) {
    const settings = await this.getSettings(userId);
    if (settings.isLocked) {
      throw new ForbiddenException('策略已锁定，无法编辑');
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
}
