import { Injectable, Logger } from '@nestjs/common';
import { Cron } from '@nestjs/schedule';
import { PrismaService } from '../../prisma/prisma.service';
import { AlertType, Role } from '../../../generated/prisma/client';

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

    // 同一事项只保留最新一条：先解决该用户的旧 BM 提醒，再创建新一条
    await this.resolvePreviousAlerts(setting.userId, AlertType.BM_REMINDER, null);

    await this.prisma.alert.create({
      data: {
        userId: setting.userId,
        type: AlertType.BM_REMINDER,
        relatedId: null,
        message: `【易护提醒】${daysText}，请及时打卡或联系护理人员。`,
      },
    });

    await this.notifyGroupNurses(
      setting.userId,
      AlertType.BM_REMINDER,
      `【易护提醒】${daysText}，请及时打卡或联系护理人员。`,
      // BM 副本用患者 ID 作 relatedId：护士同时护理多名患者时按患者区分事项
      setting.userId,
    );

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
      const remainder = roundedHours % intervalHours;
      if (roundedHours > 0 && (remainder < 1 / 60 || intervalHours - remainder < 1 / 60)) {
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
        createdAt: { gte: new Date(now.getTime() - 1 * 60 * 1000) },
      },
    });
    if (existingAlert) return;

    // 同一事项只保留最新一条：先解决该用户同药品的旧提醒，再创建新一条
    await this.resolvePreviousAlerts(userId, AlertType.MEDICATION_REMINDER, schedule.medicationId);

    await this.prisma.alert.create({
      data: {
        userId,
        type: AlertType.MEDICATION_REMINDER,
        relatedId: schedule.medicationId,
        message: `【易护提醒】请按时服用「${schedule.medication.name}」${schedule.medication.dosage || ''}`,
      },
    });

    await this.notifyGroupNurses(
      userId,
      AlertType.MEDICATION_REMINDER,
      `【易护提醒】请按时服用「${schedule.medication.name}」${schedule.medication.dosage || ''}`,
      schedule.medicationId,
    );

    this.logger.log(`Medication alert created for user ${userId}`);
  }

  /** Auto-resolve stale alerts: BM > 7 days, Medication > 2 days */
  @Cron('0 3 * * *') // daily at 3am
  async cleanupStaleAlerts() {
    const weekAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000);
    const twoDaysAgo = new Date(Date.now() - 2 * 24 * 60 * 60 * 1000);

    const [bmCount, medCount] = await Promise.all([
      this.prisma.alert.updateMany({
        where: {
          type: AlertType.BM_REMINDER,
          isResolved: false,
          createdAt: { lt: weekAgo },
        },
        data: { isResolved: true, resolvedAt: new Date() },
      }),
      this.prisma.alert.updateMany({
        where: {
          type: AlertType.MEDICATION_REMINDER,
          isResolved: false,
          createdAt: { lt: twoDaysAgo },
        },
        data: { isResolved: true, resolvedAt: new Date() },
      }),
    ]);

    if (bmCount.count > 0 || medCount.count > 0) {
      this.logger.log(
        `Stale alert cleanup: BM=${bmCount.count}, Med=${medCount.count}`,
      );
    }
  }

  /**
   * 同一事项只保留最新一条提醒：创建新 alert 前，把该用户同事项
   * （BM：relatedId=null；药品：relatedId=medicationId）的旧未解决 alert 全部解决。
   */
  private async resolvePreviousAlerts(
    userId: string,
    type: AlertType,
    relatedId: string | null,
  ) {
    await this.prisma.alert.updateMany({
      where: { userId, type, relatedId, isResolved: false },
      data: { isResolved: true, resolvedAt: new Date() },
    });
  }

  /**
   * 警报投递：为患者所在小组中的护理人员（role=NURSE，跨组去重）各创建一条副本，
   * 护士端轮询 /alerts 即可收到响铃通知。消息带患者姓名便于识别。
   * relatedId（护士副本的去重维度）：药品 = medicationId（全局唯一，天然含患者维度）；
   * BM = patientId（护士护理多名患者时按患者区分，否则互相清除）。
   */
  private async notifyGroupNurses(patientId: string, type: AlertType, message: string, relatedId: string | null) {
    try {
      const patient = await this.prisma.user.findUnique({
        where: { id: patientId },
        select: { displayName: true },
      });

      const memberships = await this.prisma.groupMember.findMany({
        where: { userId: patientId },
        include: {
          group: {
            include: {
              members: {
                where: { user: { role: Role.NURSE } },
                include: { user: { select: { id: true, displayName: true } } },
              },
            },
          },
        },
      });

      const notified = new Set<string>();
      for (const membership of memberships) {
        for (const nurse of membership.group.members) {
          if (notified.has(nurse.userId)) continue;
          notified.add(nurse.userId);
          // 护士端同一事项同样只保留最新一条副本
          await this.resolvePreviousAlerts(nurse.userId, type, relatedId);
          await this.prisma.alert.create({
            data: {
              userId: nurse.userId,
              type,
              relatedId,
              message: `【患者：${patient?.displayName || patientId}】${message}`,
            },
          });
          this.logger.log(
            `Alert forwarded to nurse ${nurse.userId} (patient ${patientId})`,
          );
        }
      }
    } catch (err: any) {
      this.logger.error(`Failed to notify nurses for patient ${patientId}: ${err.message}`);
    }
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
