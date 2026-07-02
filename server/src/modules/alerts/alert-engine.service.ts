import { Injectable, Logger } from '@nestjs/common';
import { Cron } from '@nestjs/schedule';
import { PrismaService } from '../../prisma/prisma.service';
import { AlertType } from '../../../generated/prisma/client';

@Injectable()
export class AlertEngineService {
  private readonly logger = new Logger(AlertEngineService.name);

  constructor(private prisma: PrismaService) {}

  /**
   * Check all enabled BM reminder settings every 5 minutes.
   * Creates alerts for patients who haven't recorded bowel movements
   * within their configured interval.
   */
  @Cron('*/5 * * * *')
  async checkBmReminders() {
    this.logger.debug('Running BM reminder check...');

    const settings = await this.prisma.bmReminderSetting.findMany({
      where: { isEnabled: true },
      include: { user: true },
    });

    const now = new Date();
    const currentTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;

    for (const setting of settings) {
      // Only trigger near the configured reminder time (±5 min window)
      if (!this.isTimeMatch(currentTime, setting.reminderTime)) continue;

      // Calculate check window: intervalDays - 1 days back
      const checkDays = setting.intervalDays - 1;
      const checkSince = new Date(now);
      checkSince.setDate(checkSince.getDate() - checkDays);
      checkSince.setHours(0, 0, 0, 0);

      // Check if there's a BM record in the window
      const recentRecord = await this.prisma.bmRecord.findFirst({
        where: {
          userId: setting.userId,
          recordedAt: { gte: checkSince },
        },
      });

      if (recentRecord) continue; // User has recorded, skip

      // Check if there's already an unresolved alert (dedup)
      const existingAlert = await this.prisma.alert.findFirst({
        where: {
          userId: setting.userId,
          type: AlertType.BM_REMINDER,
          isResolved: false,
          createdAt: { gte: new Date(now.getTime() - 5 * 60 * 1000) }, // 5 min dedup
        },
      });

      if (existingAlert) continue;

      // Create alert
      const daysText =
        setting.intervalDays === 1
          ? '今天还未记录大便'
          : `已连续${setting.intervalDays - 1}天未记录大便`;
      await this.prisma.alert.create({
        data: {
          userId: setting.userId,
          type: AlertType.BM_REMINDER,
          message: `【易护提醒】${daysText}，请及时打卡或联系护理人员。`,
        },
      });

      this.logger.log(
        `BM alert created for user ${setting.userId} (interval: ${setting.intervalDays}d)`,
      );
    }
  }

  /**
   * Check medication schedules every minute.
   */
  @Cron('* * * * *')
  async checkMedicationReminders() {
    this.logger.debug('Running medication reminder check...');

    const schedules = await this.prisma.medicationSchedule.findMany({
      where: { isEnabled: true },
      include: { medication: true },
    });

    const now = new Date();
    const currentTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;

    for (const schedule of schedules) {
      const userId = schedule.medication.userId;
      let shouldRemind = false;

      if (schedule.scheduleType === 'FIXED_TIME') {
        const times = schedule.fixedTimes as string[] | null;
        if (times && this.isTimeMatch(currentTime, ...times)) {
          shouldRemind = true;
        }
      } else if (schedule.scheduleType === 'INTERVAL') {
        const intervalHours = schedule.intervalHours || 8;
        const startTime = schedule.startTime || now;
        const hoursSinceStart =
          (now.getTime() - new Date(startTime).getTime()) / (1000 * 60 * 60);
        if (
          hoursSinceStart > 0 &&
          hoursSinceStart % intervalHours < 1 / 60 // ~1 min window
        ) {
          shouldRemind = true;
        }
      }

      if (!shouldRemind) continue;

      // Check last record for this medication
      const lastRecord = await this.prisma.medicationRecord.findFirst({
        where: { medicationId: schedule.medicationId, userId },
        orderBy: { takenAt: 'desc' },
      });

      // If recorded within the last hour, skip
      if (
        lastRecord &&
        now.getTime() - lastRecord.takenAt.getTime() < 60 * 60 * 1000
      ) {
        continue;
      }

      // Dedup: existing unresolved alert from last 5 min
      const existingAlert = await this.prisma.alert.findFirst({
        where: {
          userId,
          type: AlertType.MEDICATION_REMINDER,
          isResolved: false,
          createdAt: { gte: new Date(now.getTime() - 5 * 60 * 1000) },
        },
      });

      if (existingAlert) continue;

      await this.prisma.alert.create({
        data: {
          userId,
          type: AlertType.MEDICATION_REMINDER,
          message: `【易护提醒】请按时服用「${schedule.medication.name}」${schedule.medication.dosage || ''}`,
        },
      });

      this.logger.log(
        `Medication alert created for user ${userId}, med: ${schedule.medication.name}`,
      );
    }
  }

  private isTimeMatch(current: string, ...targets: string[]): boolean {
    const [cH, cM] = current.split(':').map(Number);
    return targets.some((t) => {
      const [tH, tM] = t.split(':').map(Number);
      return cH === tH && Math.abs(cM - tM) <= 2; // ±2 min tolerance
    });
  }
}
