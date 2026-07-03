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
Object.defineProperty(exports, "__esModule", { value: true });
exports.AlertsService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../../prisma/prisma.service");
let AlertsService = class AlertsService {
    prisma;
    constructor(prisma) {
        this.prisma = prisma;
    }
    async findByUser(userId, filter) {
        const where = { userId };
        if (filter === 'active') {
            where.isResolved = false;
        }
        return this.prisma.alert.findMany({
            where,
            orderBy: { createdAt: 'desc' },
            take: 50,
        });
    }
    async resolve(userId, alertId) {
        const alert = await this.prisma.alert.findUnique({
            where: { id: alertId },
        });
        if (!alert || alert.userId !== userId) {
            throw new common_1.NotFoundException('警报不存在');
        }
        return this.prisma.alert.update({
            where: { id: alertId },
            data: { isResolved: true, resolvedAt: new Date() },
        });
    }
    async getActiveCount(userId) {
        return this.prisma.alert.count({
            where: { userId, isResolved: false },
        });
    }
};
exports.AlertsService = AlertsService;
exports.AlertsService = AlertsService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService])
], AlertsService);
//# sourceMappingURL=alerts.service.js.map