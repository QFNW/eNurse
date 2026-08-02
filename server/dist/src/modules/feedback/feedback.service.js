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
exports.FeedbackService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../../prisma/prisma.service");
const email_service_1 = require("../auth/email.service");
const config_1 = require("@nestjs/config");
let FeedbackService = class FeedbackService {
    prisma;
    emailService;
    configService;
    constructor(prisma, emailService, configService) {
        this.prisma = prisma;
        this.emailService = emailService;
        this.configService = configService;
    }
    async create(userId, content) {
        const user = await this.prisma.user.findUnique({
            where: { id: userId },
            select: { id: true, email: true, nickname: true, displayName: true },
        });
        if (!user)
            throw new common_1.NotFoundException('用户不存在');
        await this.prisma.feedback.create({
            data: { userId, content },
        });
        const identity = [user.displayName, user.nickname, user.email].filter(Boolean).join(' / ');
        await this.emailService.sendMail(this.configService.get('smtp.user') || 'feedback@enurse.local', '易护 eNurse - 用户反馈', `
        <div style="max-width:480px;margin:0 auto;padding:32px;font-family:Arial,sans-serif">
          <h2 style="color:#1565C0">易护 eNurse - 用户反馈</h2>
          <p><strong>用户：</strong>${identity}</p>
          <p><strong>内容：</strong></p>
          <div style="padding:16px 24px;background:#F5F5F5;border-radius:8px;white-space:pre-wrap">${content}</div>
        </div>
      `);
        return { message: '反馈已提交，感谢您的建议' };
    }
};
exports.FeedbackService = FeedbackService;
exports.FeedbackService = FeedbackService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService,
        email_service_1.EmailService,
        config_1.ConfigService])
], FeedbackService);
//# sourceMappingURL=feedback.service.js.map