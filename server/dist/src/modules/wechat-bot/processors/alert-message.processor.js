"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
var AlertMessageProcessor_1;
Object.defineProperty(exports, "__esModule", { value: true });
exports.AlertMessageProcessor = void 0;
const bull_1 = require("@nestjs/bull");
const common_1 = require("@nestjs/common");
const wechat_bot_service_1 = require("../wechat-bot.service");
const prisma_service_1 = require("../../../prisma/prisma.service");
const client_1 = require("../../../../generated/prisma/client");
let AlertMessageProcessor = AlertMessageProcessor_1 = class AlertMessageProcessor {
    prisma;
    wechatBot;
    alertsQueue;
    logger = new common_1.Logger(AlertMessageProcessor_1.name);
    constructor(prisma, wechatBot, alertsQueue) {
        this.prisma = prisma;
        this.wechatBot = wechatBot;
        this.alertsQueue = alertsQueue;
    }
    async handleSendAlert(job) {
        const { alertId, userId, type, message, retryCount } = job.data;
        this.logger.log(`Processing alert ${alertId} (${type}) for user ${userId}, retry: ${retryCount}`);
        const alert = await this.prisma.alert.findUnique({
            where: { id: alertId },
        });
        if (!alert || alert.isResolved) {
            this.logger.log(`Alert ${alertId} already resolved, skipping`);
            return { skipped: true, reason: 'resolved' };
        }
        const user = await this.prisma.user.findUnique({
            where: { id: userId },
            select: { wechatWxid: true },
        });
        let privateSent = false;
        if (user?.wechatWxid) {
            privateSent = await this.wechatBot.sendPrivateMessage(user.wechatWxid, message);
        }
        const membership = await this.prisma.groupMember.findFirst({
            where: { userId },
            include: { group: true },
        });
        if (membership) {
            const existingGA = await this.prisma.groupAlert.findFirst({
                where: { groupId: membership.groupId, alertId },
            });
            if (!existingGA) {
                await this.prisma.groupAlert.create({
                    data: { groupId: membership.groupId, alertId },
                });
                this.logger.log(`Group alert recorded for group ${membership.groupId} (user ${userId})`);
            }
        }
        await this.prisma.sentAlert.create({
            data: {
                alertId,
                channel: client_1.AlertChannel.WECHAT_PRIVATE,
                retryCount,
                maxRetry: 6,
                isAcked: privateSent,
            },
        });
        return { privateSent };
    }
    async handleScheduleRetry(job) {
        const { alertId, userId, type, message, retryCount } = job.data;
        const maxRetry = 6;
        if (retryCount >= maxRetry) {
            this.logger.log(`Alert ${alertId} reached max retry (${maxRetry}) — auto-resolving`);
            await this.prisma.alert.update({
                where: { id: alertId },
                data: { isResolved: true, resolvedAt: new Date() },
            });
            return { stopped: true, reason: 'max_retry_reached_resolved' };
        }
        const alert = await this.prisma.alert.findUnique({
            where: { id: alertId },
        });
        if (!alert || alert.isResolved) {
            this.logger.log(`Alert ${alertId} resolved, skipping retry`);
            return { skipped: true, reason: 'resolved' };
        }
        const delay = type === 'MEDICATION_REMINDER'
            ? 15 * 60 * 1000
            : 2 * 60 * 60 * 1000;
        const nextRetry = retryCount + 1;
        await this.alertsQueue.add('send-wechat-alert', { alertId, userId, type, message, retryCount: nextRetry, isRetry: true });
        await this.alertsQueue.add('schedule-retry', { alertId, userId, type, message, retryCount: nextRetry, isRetry: true }, { delay });
        this.logger.log(`Retry #${nextRetry} for alert ${alertId} in ${delay / 1000}s`);
        return { scheduled: true, nextRetryIn: delay, attempt: nextRetry };
    }
};
exports.AlertMessageProcessor = AlertMessageProcessor;
__decorate([
    (0, bull_1.Process)('send-wechat-alert'),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], AlertMessageProcessor.prototype, "handleSendAlert", null);
__decorate([
    (0, bull_1.Process)('schedule-retry'),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], AlertMessageProcessor.prototype, "handleScheduleRetry", null);
exports.AlertMessageProcessor = AlertMessageProcessor = AlertMessageProcessor_1 = __decorate([
    (0, bull_1.Processor)('alerts'),
    __param(2, (0, bull_1.InjectQueue)('alerts')),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService,
        wechat_bot_service_1.WechatBotService, Object])
], AlertMessageProcessor);
//# sourceMappingURL=alert-message.processor.js.map