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

    const settings = await this.prisma.bmReminderSetting.findMany({
      where: { isEnabled: true },
    });

    const now = new Date();
    const currentTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;

    for (const setting of settings) {
      if (!this.isTimeMatch(currentTime, setting.reminderTime)) continue;

      const checkDays = setting.intervalDays - 1;
      const checkSince = new Date(now);
      checkSince.setDate(checkSince.getDate() - checkDays);
      checkSince.setHours(0, 0, 0, 0);

      const recentRecord = await this.prisma.bmRecord.findFirst({
        where: { userId: setting.userId, recordedAt: { gte: checkSince } },
      });

      if (recentRecord) continue;

      // Dedup: existing unresolved alert from last 5 min
      const existingAlert = await this.prisma.alert.findFirst({
        where: {
          userId: setting.userId,
          type: AlertType.BM_REMINDER,
          isResolved: false,
          createdAt: { gte: new Date(now.getTime() - 5 * 60 * 1000) },
        },
      });

      if (existingAlert) continue;

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

      // Push to WeChat Bot queue
      await this.enqueueAlert(alert.id, setting.userId, alert.type, alert.message);
    }
  }

  /** Check medication schedules every minute */
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
        if (hoursSinceStart > 0 && hoursSinceStart % intervalHours < 1 / 60) {
          shouldRemind = true;
        }
      }

      if (!shouldRemind) continue;

      const lastRecord = await this.prisma.medicationRecord.findFirst({
        where: { medicationId: schedule.medicationId, userId },
        orderBy: { takenAt: 'desc' },
      });

      if (lastRecord && now.getTime() - lastRecord.takenAt.getTime() < 60 * 60 * 1000) {
        continue;
      }

      const existingAlert = await this.prisma.alert.findFirst({
        where: {
          userId,
          type: AlertType.MEDICATION_REMINDER,
          isResolved: false,
          createdAt: { gte: new Date(now.getTime() - 5 * 60 * 1000) },
        },
      });

      if (existingAlert) continue;

      const alert = await this.prisma.alert.create({
        data: {
          userId,
          type: AlertType.MEDICATION_REMINDER,
          message: `【易护提醒】请按时服用「${schedule.medication.name}」${schedule.medication.dosage || ''}`,
        },
      });

      this.logger.log(`Medication alert ${alert.id} created for user ${userId}`);

      // Push to WeChat Bot queue
      await this.enqueueAlert(alert.id, userId, alert.type, alert.message);
    }
  }

  /** Push alert to Bull queue for WeChat sending + schedule retry */
  private async enqueueAlert(
    alertId: string,
    userId: string,
    type: AlertType,
    message: string,
  ) {
    // 1. Immediate send
    await this.alertsQueue.add('send-wechat-alert', {
      alertId,
      userId,
      type,
      message,
      retryCount: 0,
      isRetry: false,
    } as AlertJobData);

    // 2. Schedule retry based on alert type
    const retryDelay =
      type === AlertType.MEDICATION_REMINDER
        ? 15 * 60 * 1000   // 15 min
        : 2 * 60 * 60 * 1000; // 2 hours

    await this.alertsQueue.add(
      'schedule-retry',
      { alertId, userId, type, message, retryCount: 0, isRetry: true } as AlertJobData,
      { delay: retryDelay },
    );

    this.logger.log(
      `Alert ${alertId} enqueued — retry in ${retryDelay / 1000}s`,
    );
  }

  private isTimeMatch(current: string, ...targets: string[]): boolean {
    const [cH, cM] = current.split(':').map(Number);
    return targets.some((t) => {
      const [tH, tM] = t.split(':').map(Number);
      return cH === tH && Math.abs(cM - tM) <= 2;
    });
  }
}
