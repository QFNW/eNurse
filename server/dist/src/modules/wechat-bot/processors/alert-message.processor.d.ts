import type { Job, Queue } from 'bull';
import { WechatBotService } from '../wechat-bot.service';
import { PrismaService } from '../../../prisma/prisma.service';
import { AlertType } from '../../../../generated/prisma/client';
export interface AlertJobData {
    alertId: string;
    userId: string;
    type: AlertType;
    message: string;
    retryCount: number;
    isRetry: boolean;
}
export declare class AlertMessageProcessor {
    private prisma;
    private wechatBot;
    private alertsQueue;
    private readonly logger;
    constructor(prisma: PrismaService, wechatBot: WechatBotService, alertsQueue: Queue);
    handleSendAlert(job: Job<AlertJobData>): Promise<{
        skipped: boolean;
        reason: string;
        privateSent?: undefined;
    } | {
        privateSent: boolean;
        skipped?: undefined;
        reason?: undefined;
    }>;
    handleScheduleRetry(job: Job<AlertJobData>): Promise<{
        stopped: boolean;
        reason: string;
        skipped?: undefined;
        scheduled?: undefined;
        nextRetryIn?: undefined;
        attempt?: undefined;
    } | {
        skipped: boolean;
        reason: string;
        stopped?: undefined;
        scheduled?: undefined;
        nextRetryIn?: undefined;
        attempt?: undefined;
    } | {
        scheduled: boolean;
        nextRetryIn: number;
        attempt: number;
        stopped?: undefined;
        reason?: undefined;
        skipped?: undefined;
    }>;
}
