"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.EmailService = void 0;
const common_1 = require("@nestjs/common");
const config_1 = require("@nestjs/config");
const cache_service_1 = require("../cache/cache.service");
const nodemailer = __importStar(require("nodemailer"));
let EmailService = class EmailService {
    configService;
    cacheService;
    transporter = null;
    constructor(configService, cacheService) {
        this.configService = configService;
        this.cacheService = cacheService;
        const user = this.configService.get('smtp.user');
        const pass = this.configService.get('smtp.pass');
        if (user && pass) {
            const port = this.configService.get('smtp.port') || 465;
            this.transporter = nodemailer.createTransport({
                host: this.configService.get('smtp.host'),
                port,
                secure: port === 465,
                auth: { user, pass },
            });
        }
        else {
            console.warn('SMTP not configured — verification codes will be logged to console');
        }
    }
    async sendVerificationCode(email) {
        const code = String(Math.floor(100000 + Math.random() * 900000));
        await this.cacheService.setCode(email, code, 300);
        if (this.transporter) {
            await this.transporter.sendMail({
                from: this.configService.get('smtp.user'),
                to: email,
                subject: '易护 eNurse - 邮箱验证码',
                html: `
          <div style="max-width:480px;margin:0 auto;padding:32px;font-family:Arial,sans-serif">
            <h2 style="color:#1565C0">易护 eNurse</h2>
            <p>您的验证码是：</p>
            <div style="font-size:32px;font-weight:bold;color:#1565C0;padding:16px 24px;background:#E3F2FD;border-radius:12px;text-align:center;letter-spacing:6px">${code}</div>
            <p style="margin-top:24px;color:#666">有效期 5 分钟，请勿泄露。</p>
          </div>
        `,
            });
        }
        else {
            console.log(`[EmailService] 验证码 for ${email}: ${code}`);
        }
    }
    async verifyCode(email, code) {
        const stored = await this.cacheService.getCode(email);
        return stored !== null && stored === code;
    }
    async consumeCode(email, code) {
        const valid = await this.verifyCode(email, code);
        if (valid) {
            await this.cacheService.deleteCode(email);
        }
        return valid;
    }
};
exports.EmailService = EmailService;
exports.EmailService = EmailService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [config_1.ConfigService,
        cache_service_1.CacheService])
], EmailService);
//# sourceMappingURL=email.service.js.map