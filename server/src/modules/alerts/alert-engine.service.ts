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
      }
    }
  }

  private async checkSingleBmReminder(
    setting: any,
    now: Date,
    currentTime: string,
  ) {
    // Guard: intervalDays must be >= 1
    const intervalDays = Math.max(1, setting.intervalDays || 1);
    if (setting.intervalDays < 1) {
      this.logger.warn(
        `User ${setting.userId} has intervalDays=${setting.intervalDays}, clamping to 1`,
      );
    }

    // BM uses ±4 min tolerance to cover all 5-min cron slots including min 59
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

    const daysText =
      intervalDays === 1
        ? '今天还未记录大便'
        : `已连续${intervalDays - 1}天未记录大便`;

    const alert = await this.prisma.alert.create({
      data: {
        userId: setting.userId,
        type: AlertType.BM_REMINDER,
        message: `【易护提醒】${daysText}，请及时打卡或联系护理人员。`,
      },
    });

    this.logger.log(`BM alert ${alert.id} created for user ${setting.userId}`);

    // If enqueue fails, delete the alert so dedup doesn't block retry next cron
    const enqueued = await this.enqueueAlert(
      alert.id,
      setting.userId,
      alert.type,
      alert.message,
    );
    if (!enqueued) {
      try {
        await this.prisma.alert.delete({ where: { id: alert.id } });
        this.logger.warn(`Alert ${alert.id} deleted — will retry next cron`);
      } catch (delErr: any) {
        this.logger.error(
          `Failed to delete unenqueued alert ${alert.id}: ${delErr.message} — ` +
          'alert will be skipped by dedup until it expires, then recreated',
        );
      }
    }
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
      // Medication uses ±1 min tolerance (1-min cron)
      if (times.length > 0 && this.isTimeMatch(currentTime, 1, ...times)) {
        shouldRemind = true;
      }
    } else if (schedule.scheduleType === 'INTERVAL') {
      const intervalHours = schedule.intervalHours || 8;
      if (!schedule.startTime) {
        this.logger.warn(
          `INTERVAL schedule ${schedule.id} has no startTime — auto-setting to now`,
        );
        await this.prisma.medicationSchedule.update({
          where: { id: schedule.id },
          data: { startTime: now },
        });
        schedule.startTime = now;
      }
      const hoursSinceStart =
        (now.getTime() - new Date(schedule.startTime).getTime()) /
        (1000 * 60 * 60);
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

    const enqueued = await this.enqueueAlert(
      alert.id,
      userId,
      alert.type,
      alert.message,
    );
    if (!enqueued) {
      try {
        await this.prisma.alert.delete({ where: { id: alert.id } });
        this.logger.warn(`Alert ${alert.id} deleted — will retry next cron`);
      } catch (delErr: any) {
        this.logger.error(
          `Failed to delete unenqueued alert ${alert.id}: ${delErr.message} — ` +
          'alert will be skipped by dedup until it expires, then recreated',
        );
      }
    }
  }

  /**
   * Push alert to Bull queue. Returns true if successful, false if failed.
   */
  private async enqueueAlert(
    alertId: string,
    userId: string,
    type: AlertType,
    message: string,
  ): Promise<boolean> {
    // Phase 1: enqueue the immediate send (critical)
    try {
      await this.alertsQueue.add('send-wechat-alert', {
        alertId, userId, type, message,
        retryCount: 0, isRetry: false,
      } as AlertJobData);
    } catch (err: any) {
      this.logger.error(`Failed to enqueue send for alert ${alertId}: ${err.message}`);
      return false;
    }

    // Phase 2: schedule retry (best-effort, non-critical)
    const retryDelay =
      type === AlertType.MEDICATION_REMINDER ? 15 * 60 * 1000 : 2 * 60 * 60 * 1000;
    try {
      await this.alertsQueue.add(
        'schedule-retry',
        { alertId, userId, type, message, retryCount: 0, isRetry: true } as AlertJobData,
        { delay: retryDelay },
      );
      this.logger.log(`Alert ${alertId} enqueued — retry in ${retryDelay / 1000}s`);
    } catch (err: any) {
      this.logger.warn(`Failed to schedule retry for alert ${alertId}: ${err.message} — will retry via next cron`);
    }

    return true;
  }

  /**
   * Safely parse fixedTimes from JSON, returning only valid HH:MM strings.
   */
  private parseFixedTimes(raw: any): string[] {
    if (!Array.isArray(raw)) return [];
    return raw.filter(
      (t): t is string =>
        typeof t === 'string' && /^([01]\d|2[0-3]):[0-5]\d$/.test(t),
    );
  }

  /**
   * Check if current time matches any target time within tolerance minutes.
   * @param toleranceMinutes ± minutes to consider a match
   */
  private isTimeMatch(
    current: string,
    toleranceMinutes: number,
    ...targets: string[]
  ): boolean {
    const [cH, cM] = current.split(':').map(Number);
    const currentTotal = cH * 60 + cM;
    return targets.some((t) => {
      const [tH, tM] = t.split(':').map(Number);
      const targetTotal = tH * 60 + tM;
      return Math.abs(currentTotal - targetTotal) <= toleranceMinutes;
    });
  }
}
