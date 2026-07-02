import { Injectable, Logger } from '@nestjs/common';
import { Cron } from '@nestjs/schedule';
import { InjectQueue } from '@nestjs/bull';
import type { Queue } from 'bull';
import { PrismaService } from '../../prisma/prisma.service';
import { AlertType } from '../../../generated/prisma/client';
import type { AlertJobData } from '../wechat-bot/processors/alert-message.processor';

@Injectable()
export class AlertEngineService {
  private readonly logger = new Logger(AlertEngineService.name);

  constructor(
    private prisma: PrismaService,
    @InjectQueue('alerts') private alertsQueue: Queue,
  ) {}

  /** Check BM reminders every 5 minutes */
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
        this.logger.error(
          `BM check failed for user ${setting.userId}: ${err.message}`,
        );
        // Continue to next user instead of aborting all
      }
    }
  }

  private async checkSingleBmReminder(
    setting: any,
    now: Date,
    currentTime: string,
  ) {
    if (!this.isTimeMatch(currentTime, setting.reminderTime)) return;

    const checkDays = setting.intervalDays - 1;
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

    const daysText =
      setting.intervalDays === 1
        ? '今天还未记录大便'
        : `已连续${setting.intervalDays - 1}天未记录大便`;

    const alert = await this.prisma.alert.create({
      data: {
        userId: setting.userId,
        type: AlertType.BM_REMINDER,
        message: `【易护提醒】${daysText}，请及时打卡或联系护理人员。`,
      },
    });

    this.logger.log(`BM alert ${alert.id} created for user ${setting.userId}`);

    await this.enqueueAlert(alert.id, setting.userId, alert.type, alert.message);
  }

  /** Check medication schedules every minute */
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
        this.logger.error(
          `Medication check failed for medication ${schedule.medicationId}: ${err.message}`,
        );
        // Continue to next schedule
      }
    }
  }

  private async checkSingleMedicationReminder(
    schedule: any,
    now: Date,
    currentTime: string,
  ) {
    const userId = schedule.medication.userId;
    let shouldRemind = false;

    if (schedule.scheduleType === 'FIXED_TIME') {
      const times = this.parseFixedTimes(schedule.fixedTimes);
      if (times.length > 0 && this.isTimeMatch(currentTime, ...times)) {
        shouldRemind = true;
      }
    } else if (schedule.scheduleType === 'INTERVAL') {
      const intervalHours = schedule.intervalHours || 8;
      // Only trigger if startTime is set; skip if null (no default to now)
      if (!schedule.startTime) return;
      const hoursSinceStart =
        (now.getTime() - new Date(schedule.startTime).getTime()) / (1000 * 60 * 60);
      // Use a 1-minute window: round to nearest minute to avoid float issues
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

    if (
      lastRecord &&
      now.getTime() - lastRecord.takenAt.getTime() < 60 * 60 * 1000
    ) {
      return;
    }

    const existingAlert = await this.prisma.alert.findFirst({
      where: {
        userId,
        type: AlertType.MEDICATION_REMINDER,
        isResolved: false,
        createdAt: { gte: new Date(now.getTime() - 5 * 60 * 1000) },
      },
    });

    if (existingAlert) return;

    const alert = await this.prisma.alert.create({
      data: {
        userId,
        type: AlertType.MEDICATION_REMINDER,
        message: `【易护提醒】请按时服用「${schedule.medication.name}」${schedule.medication.dosage || ''}`,
      },
    });

    this.logger.log(`Medication alert ${alert.id} created for user ${userId}`);

    await this.enqueueAlert(alert.id, userId, alert.type, alert.message);
  }

  /** Push alert to Bull queue for WeChat sending + schedule retry */
  private async enqueueAlert(
    alertId: string,
    userId: string,
    type: AlertType,
    message: string,
  ) {
    try {
      await this.alertsQueue.add('send-wechat-alert', {
        alertId,
        userId,
        type,
        message,
        retryCount: 0,
        isRetry: false,
      } as AlertJobData);

      const retryDelay =
        type === AlertType.MEDICATION_REMINDER
          ? 15 * 60 * 1000
          : 2 * 60 * 60 * 1000;

      await this.alertsQueue.add(
        'schedule-retry',
        { alertId, userId, type, message, retryCount: 0, isRetry: true } as AlertJobData,
        { delay: retryDelay },
      );

      this.logger.log(`Alert ${alertId} enqueued — retry in ${retryDelay / 1000}s`);
    } catch (err: any) {
      this.logger.error(
        `Failed to enqueue alert ${alertId}: ${err.message}. ` +
        'Alert exists in DB but was not sent to WeChat. Will be picked up on next cron.',
      );
    }
  }

  /**
   * Safely parse fixedTimes from JSON, returning only valid string arrays.
   */
  private parseFixedTimes(raw: any): string[] {
    if (!Array.isArray(raw)) return [];
    return raw.filter(
      (t): t is string =>
        typeof t === 'string' && /^\d{1,2}:\d{2}$/.test(t),
    );
  }

  private isTimeMatch(current: string, ...targets: string[]): boolean {
    const [cH, cM] = current.split(':').map(Number);
    return targets.some((t) => {
      const [tH, tM] = t.split(':').map(Number);
      // Exact hour match + within 1 minute to prevent 3-min duplicate window
      return cH === tH && Math.abs(cM - tM) <= 1;
    });
  }
}
