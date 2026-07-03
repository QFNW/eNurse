import { Injectable, Logger } from '@nestjs/common';
import { Cron } from '@nestjs/schedule';
import { PrismaService } from '../../prisma/prisma.service';
import { AlertType } from '../../../generated/prisma/client';

@Injectable()
export class AlertEngineService {
  private readonly logger = new Logger(AlertEngineService.name);

  constructor(private prisma: PrismaService) {}

  @Cron('*/5 * * * *')
  async checkBmReminders() {
    this.logger.debug('Running BM reminder check...');

    let settings;
    try {
      settings = await this.prisma.bmReminderSetting.findMany({
        where: { isEnabled: true },
      });
    } catch (err: any) {
      this.logger.error(`Failed to fetch BM settings: ${err.message}`);
      return;
    }

    const now = new Date();
    const currentTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;

    for (const setting of settings) {
      try {
        await this.checkSingleBmReminder(setting, now, currentTime);
      } catch (err: any) {
        this.logger.error(`BM check failed for user ${setting.userId}: ${err.message}`);
      }
    }
  }

  private async checkSingleBmReminder(setting: any, now: Date, currentTime: string) {
    const intervalDays = Math.max(1, setting.intervalDays || 1);
    if (setting.intervalDays < 1) {
      this.logger.warn(`User ${setting.userId} has intervalDays=${setting.intervalDays}, clamping to 1`);
    }

    if (!this.isTimeMatch(currentTime, 4, setting.reminderTime)) return;

    const checkDays = intervalDays - 1;
    const checkSince = new Date(now);
    checkSince.setDate(checkSince.getDate() - checkDays);
    checkSince.setHours(0, 0, 0, 0);

    const recentRecord = await this.prisma.bmRecord.findFirst({
      where: { userId: setting.userId, recordedAt: { gte: checkSince } },
    });
    if (recentRecord) return;

    const existingAlert = await this.prisma.alert.findFirst({
      where: {
        userId: setting.userId,
        type: AlertType.BM_REMINDER,
        isResolved: false,
        createdAt: { gte: new Date(now.getTime() - 5 * 60 * 1000) },
      },
    });
    if (existingAlert) return;

    const daysText = intervalDays === 1
      ? '今天还未记录大便'
      : `已连续${intervalDays - 1}天未记录大便`;

    await this.prisma.alert.create({
      data: {
        userId: setting.userId,
        type: AlertType.BM_REMINDER,
        message: `【易护提醒】${daysText}，请及时打卡或联系护理人员。`,
      },
    });

    this.logger.log(`BM alert created for user ${setting.userId}`);
  }

  @Cron('* * * * *')
  async checkMedicationReminders() {
    this.logger.debug('Running medication reminder check...');

    let schedules;
    try {
      schedules = await this.prisma.medicationSchedule.findMany({
        where: { isEnabled: true },
        include: { medication: true },
      });
    } catch (err: any) {
      this.logger.error(`Failed to fetch medication schedules: ${err.message}`);
      return;
    }

    const now = new Date();
    const currentTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;

    for (const schedule of schedules) {
      try {
        await this.checkSingleMedicationReminder(schedule, now, currentTime);
      } catch (err: any) {
        this.logger.error(`Medication check failed for ${schedule.medicationId}: ${err.message}`);
      }
    }
  }

  private async checkSingleMedicationReminder(schedule: any, now: Date, currentTime: string) {
    const userId = schedule.medication.userId;
    let shouldRemind = false;

    if (schedule.scheduleType === 'FIXED_TIME') {
      const times = this.parseFixedTimes(schedule.fixedTimes);
      if (times.length > 0 && this.isTimeMatch(currentTime, 1, ...times)) {
        shouldRemind = true;
      }
    } else if (schedule.scheduleType === 'INTERVAL') {
      const intervalHours = schedule.intervalHours || 8;
      if (!schedule.startTime) {
        this.logger.warn(`INTERVAL schedule ${schedule.id} has no startTime — auto-setting to now`);
        await this.prisma.medicationSchedule.update({
          where: { id: schedule.id },
          data: { startTime: now },
        });
        schedule.startTime = now;
      }
      const hoursSinceStart = (now.getTime() - new Date(schedule.startTime).getTime()) / (1000 * 60 * 60);
      const roundedHours = Math.round(hoursSinceStart * 60) / 60;
      if (roundedHours > 0 && roundedHours % intervalHours < 1 / 60) {
        shouldRemind = true;
      }
    }

    if (!shouldRemind) return;

    const lastRecord = await this.prisma.medicationRecord.findFirst({
      where: { medicationId: schedule.medicationId, userId },
      orderBy: { takenAt: 'desc' },
    });
    if (lastRecord && now.getTime() - lastRecord.takenAt.getTime() < 60 * 60 * 1000) return;

    const existingAlert = await this.prisma.alert.findFirst({
      where: {
        userId,
        type: AlertType.MEDICATION_REMINDER,
        isResolved: false,
        createdAt: { gte: new Date(now.getTime() - 5 * 60 * 1000) },
      },
    });
    if (existingAlert) return;

    await this.prisma.alert.create({
      data: {
        userId,
        type: AlertType.MEDICATION_REMINDER,
        message: `【易护提醒】请按时服用「${schedule.medication.name}」${schedule.medication.dosage || ''}`,
      },
    });

    this.logger.log(`Medication alert created for user ${userId}`);
  }

  private parseFixedTimes(raw: any): string[] {
    if (!Array.isArray(raw)) return [];
    return raw.filter(
      (t): t is string =>
        typeof t === 'string' && /^([01]\d|2[0-3]):[0-5]\d$/.test(t),
    );
  }

  private isTimeMatch(current: string, toleranceMinutes: number, ...targets: string[]): boolean {
    const [cH, cM] = current.split(':').map(Number);
    const currentTotal = cH * 60 + cM;
    return targets.some((t) => {
      const [tH, tM] = t.split(':').map(Number);
      return Math.abs(currentTotal - (tH * 60 + tM)) <= toleranceMinutes;
    });
  }
}
