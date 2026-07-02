import { Process, Processor, InjectQueue } from '@nestjs/bull';
import { Logger } from '@nestjs/common';
import type { Job, Queue } from 'bull';
import { WechatBotService } from '../wechat-bot.service';
import { PrismaService } from '../../../prisma/prisma.service';
import { AlertChannel, AlertType } from '../../../../generated/prisma/client';

export interface AlertJobData {
  alertId: string;
  userId: string;
  type: AlertType;
  message: string;
  retryCount: number;
  isRetry: boolean;
}

@Processor('alerts')
export class AlertMessageProcessor {
  private readonly logger = new Logger(AlertMessageProcessor.name);

  constructor(
    private prisma: PrismaService,
    private wechatBot: WechatBotService,
    @InjectQueue('alerts') private alertsQueue: Queue,
  ) {}

  @Process('send-wechat-alert')
  async handleSendAlert(job: Job<AlertJobData>) {
    const { alertId, userId, type, message, retryCount } = job.data;

    this.logger.log(
      `Processing alert ${alertId} (${type}) for user ${userId}, retry: ${retryCount}`,
    );

    // Double-check for retry: is the alert still unresolved?
    if (job.data.isRetry) {
      const alert = await this.prisma.alert.findUnique({
        where: { id: alertId },
      });
      if (!alert || alert.isResolved) {
        this.logger.log(`Alert ${alertId} already resolved, skipping`);
        return { skipped: true, reason: 'resolved' };
      }
    }

    // Look up user
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { wechatWxid: true },
    });

    let privateSent = false;

    // Send private message
    if (user?.wechatWxid) {
      privateSent = await this.wechatBot.sendPrivateMessage(
        user.wechatWxid,
        message,
      );
    }

    // Look up group membership
    const membership = await this.prisma.groupMember.findFirst({
      where: { userId },
      include: { group: true },
    });

    if (membership) {
      await this.prisma.groupAlert.create({
        data: { groupId: membership.groupId, alertId },
      });

      // Group WeChat room ID — needs to be configured per group (future)
      this.logger.log(
        `Group alert recorded for group ${membership.groupId} (user ${userId})`,
      );
    }

    // Record sent alert
    await this.prisma.sentAlert.create({
      data: {
        alertId,
        channel: AlertChannel.WECHAT_PRIVATE,
        retryCount,
        maxRetry: 6,
        isAcked: privateSent,
      },
    });

    return { privateSent };
  }

  @Process('schedule-retry')
  async handleScheduleRetry(job: Job<AlertJobData>) {
    const { alertId, userId, type, message, retryCount } = job.data;
    const maxRetry = 6;

    if (retryCount >= maxRetry) {
      this.logger.log(`Alert ${alertId} reached max retry (${maxRetry})`);
      return { stopped: true, reason: 'max_retry_reached' };
    }

    const alert = await this.prisma.alert.findUnique({
      where: { id: alertId },
    });

    if (!alert || alert.isResolved) {
      this.logger.log(`Alert ${alertId} resolved, skipping retry`);
      return { skipped: true, reason: 'resolved' };
    }

    // Re-enqueue send with incremented retry count
    const delay =
      type === 'MEDICATION_REMINDER'
        ? 15 * 60 * 1000  // 15 min
        : 2 * 60 * 60 * 1000; // 2 hours

    const nextRetry = retryCount + 1;

    await this.alertsQueue.add(
      'send-wechat-alert',
      { alertId, userId, type, message, retryCount: nextRetry, isRetry: true } as AlertJobData,
    );

    await this.alertsQueue.add(
      'schedule-retry',
      { alertId, userId, type, message, retryCount: nextRetry, isRetry: true } as AlertJobData,
      { delay },
    );

    this.logger.log(
      `Retry #${nextRetry} for alert ${alertId} in ${delay / 1000}s`,
    );

    return { scheduled: true, nextRetryIn: delay, attempt: nextRetry };
  }
}
