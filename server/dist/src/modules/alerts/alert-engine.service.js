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
var AlertEngineService_1;
Object.defineProperty(exports, "__esModule", { value: true });
exports.AlertEngineService = void 0;
const common_1 = require("@nestjs/common");
const schedule_1 = require("@nestjs/schedule");
const bull_1 = require("@nestjs/bull");
const prisma_service_1 = require("../../prisma/prisma.service");
const client_1 = require("../../../generated/prisma/client");
let AlertEngineService = AlertEngineService_1 = class AlertEngineService {
    prisma;
    alertsQueue;
    logger = new common_1.Logger(AlertEngineService_1.name);
    constructor(prisma, alertsQueue) {
        this.prisma = prisma;
        this.alertsQueue = alertsQueue;
    }
    async checkBmReminders() {
        this.logger.debug('Running BM reminder check...');
        let settings;
        try {
            settings = await this.prisma.bmReminderSetting.findMany({
                where: { isEnabled: true },
            });
        }
        catch (err) {
            this.logger.error(`Failed to fetch BM settings: ${err.message}`);
            return;
        }
        const now = new Date();
        const currentTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
        for (const setting of settings) {
            try {
                await this.checkSingleBmReminder(setting, now, currentTime);
            }
            catch (err) {
                this.logger.error(`BM check failed for user ${setting.userId}: ${err.message}`);
            }
        }
    }
    async checkSingleBmReminder(setting, now, currentTime) {
        const intervalDays = Math.max(1, setting.intervalDays || 1);
        if (setting.intervalDays < 1) {
            this.logger.warn(`User ${setting.userId} has intervalDays=${setting.intervalDays}, clamping to 1`);
        }
        if (!this.isTimeMatch(currentTime, 4, setting.reminderTime))
            return;
        const checkDays = intervalDays - 1;
        const checkSince = new Date(now);
        checkSince.setDate(checkSince.getDate() - checkDays);
        checkSince.setHours(0, 0, 0, 0);
        const recentRecord = await this.prisma.bmRecord.findFirst({
            where: { userId: setting.userId, recordedAt: { gte: checkSince } },
        });
        if (recentRecord)
            return;
        const existingAlert = await this.prisma.alert.findFirst({
            where: {
                userId: setting.userId,
                type: client_1.AlertType.BM_REMINDER,
                isResolved: false,
                createdAt: { gte: new Date(now.getTime() - 5 * 60 * 1000) },
            },
        });
        if (existingAlert)
            return;
        const daysText = intervalDays === 1
            ? '今天还未记录大便'
            : `已连续${intervalDays - 1}天未记录大便`;
        const alert = await this.prisma.alert.create({
            data: {
                userId: setting.userId,
                type: client_1.AlertType.BM_REMINDER,
                message: `【易护提醒】${daysText}，请及时打卡或联系护理人员。`,
            },
        });
        this.logger.log(`BM alert ${alert.id} created for user ${setting.userId}`);
        const enqueued = await this.enqueueAlert(alert.id, setting.userId, alert.type, alert.message);
        if (!enqueued) {
            try {
                await this.prisma.alert.delete({ where: { id: alert.id } });
                this.logger.warn(`Alert ${alert.id} deleted — will retry next cron`);
            }
            catch (delErr) {
                this.logger.error(`Failed to delete unenqueued alert ${alert.id}: ${delErr.message} — ` +
                    'alert will be skipped by dedup until it expires, then recreated');
            }
        }
    }
    async checkMedicationReminders() {
        this.logger.debug('Running medication reminder check...');
        let schedules;
        try {
            schedules = await this.prisma.medicationSchedule.findMany({
                where: { isEnabled: true },
                include: { medication: true },
            });
        }
        catch (err) {
            this.logger.error(`Failed to fetch medication schedules: ${err.message}`);
            return;
        }
        const now = new Date();
        const currentTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
        for (const schedule of schedules) {
            try {
                await this.checkSingleMedicationReminder(schedule, now, currentTime);
            }
            catch (err) {
                this.logger.error(`Medication check failed for medication ${schedule.medicationId}: ${err.message}`);
            }
        }
    }
    async checkSingleMedicationReminder(schedule, now, currentTime) {
        const userId = schedule.medication.userId;
        let shouldRemind = false;
        if (schedule.scheduleType === 'FIXED_TIME') {
            const times = this.parseFixedTimes(schedule.fixedTimes);
            if (times.length > 0 && this.isTimeMatch(currentTime, 1, ...times)) {
                shouldRemind = true;
            }
        }
        else if (schedule.scheduleType === 'INTERVAL') {
            const intervalHours = schedule.intervalHours || 8;
            if (!schedule.startTime) {
                this.logger.warn(`INTERVAL schedule ${schedule.id} has no startTime — auto-setting to now`);
                await this.prisma.medicationSchedule.update({
                    where: { id: schedule.id },
                    data: { startTime: now },
                });
                schedule.startTime = now;
            }
            const hoursSinceStart = (now.getTime() - new Date(schedule.startTime).getTime()) /
                (1000 * 60 * 60);
            const roundedHours = Math.round(hoursSinceStart * 60) / 60;
            if (roundedHours > 0 && roundedHours % intervalHours < 1 / 60) {
                shouldRemind = true;
            }
        }
        if (!shouldRemind)
            return;
        const lastRecord = await this.prisma.medicationRecord.findFirst({
            where: { medicationId: schedule.medicationId, userId },
            orderBy: { takenAt: 'desc' },
        });
        if (lastRecord &&
            now.getTime() - lastRecord.takenAt.getTime() < 60 * 60 * 1000) {
            return;
        }
        const existingAlert = await this.prisma.alert.findFirst({
            where: {
                userId,
                type: client_1.AlertType.MEDICATION_REMINDER,
                isResolved: false,
                createdAt: { gte: new Date(now.getTime() - 5 * 60 * 1000) },
            },
        });
        if (existingAlert)
            return;
        const alert = await this.prisma.alert.create({
            data: {
                userId,
                type: client_1.AlertType.MEDICATION_REMINDER,
                message: `【易护提醒】请按时服用「${schedule.medication.name}」${schedule.medication.dosage || ''}`,
            },
        });
        this.logger.log(`Medication alert ${alert.id} created for user ${userId}`);
        const enqueued = await this.enqueueAlert(alert.id, userId, alert.type, alert.message);
        if (!enqueued) {
            try {
                await this.prisma.alert.delete({ where: { id: alert.id } });
                this.logger.warn(`Alert ${alert.id} deleted — will retry next cron`);
            }
            catch (delErr) {
                this.logger.error(`Failed to delete unenqueued alert ${alert.id}: ${delErr.message} — ` +
                    'alert will be skipped by dedup until it expires, then recreated');
            }
        }
    }
    async enqueueAlert(alertId, userId, type, message) {
        try {
            await this.alertsQueue.add('send-wechat-alert', {
                alertId, userId, type, message,
                retryCount: 0, isRetry: false,
            });
        }
        catch (err) {
            this.logger.error(`Failed to enqueue send for alert ${alertId}: ${err.message}`);
            return false;
        }
        const retryDelay = type === client_1.AlertType.MEDICATION_REMINDER ? 15 * 60 * 1000 : 2 * 60 * 60 * 1000;
        try {
            await this.alertsQueue.add('schedule-retry', { alertId, userId, type, message, retryCount: 0, isRetry: true }, { delay: retryDelay });
            this.logger.log(`Alert ${alertId} enqueued — retry in ${retryDelay / 1000}s`);
        }
        catch (err) {
            this.logger.warn(`Failed to schedule retry for alert ${alertId}: ${err.message} — will retry via next cron`);
        }
        return true;
    }
    parseFixedTimes(raw) {
        if (!Array.isArray(raw))
            return [];
        return raw.filter((t) => typeof t === 'string' && /^([01]\d|2[0-3]):[0-5]\d$/.test(t));
    }
    isTimeMatch(current, toleranceMinutes, ...targets) {
        const [cH, cM] = current.split(':').map(Number);
        const currentTotal = cH * 60 + cM;
        return targets.some((t) => {
            const [tH, tM] = t.split(':').map(Number);
            const targetTotal = tH * 60 + tM;
            return Math.abs(currentTotal - targetTotal) <= toleranceMinutes;
        });
    }
};
exports.AlertEngineService = AlertEngineService;
__decorate([
    (0, schedule_1.Cron)('*/5 * * * *'),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], AlertEngineService.prototype, "checkBmReminders", null);
__decorate([
    (0, schedule_1.Cron)('* * * * *'),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], AlertEngineService.prototype, "checkMedicationReminders", null);
exports.AlertEngineService = AlertEngineService = AlertEngineService_1 = __decorate([
    (0, common_1.Injectable)(),
    __param(1, (0, bull_1.InjectQueue)('alerts')),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService, Object])
], AlertEngineService);
//# sourceMappingURL=alert-engine.service.js.map