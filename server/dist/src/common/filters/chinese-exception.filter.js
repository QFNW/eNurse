"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var ChineseExceptionFilter_1;
Object.defineProperty(exports, "__esModule", { value: true });
exports.ChineseExceptionFilter = void 0;
const common_1 = require("@nestjs/common");
let ChineseExceptionFilter = class ChineseExceptionFilter {
    static { ChineseExceptionFilter_1 = this; }
    logger = new common_1.Logger(ChineseExceptionFilter_1.name);
    static TRANSLATIONS = {
        'intervalHours must not be less than 0': '用药间隔须为正数',
        'intervalDays must not be less than 1': '大便间隔天数须 ≥ 1',
        'password must be longer than or equal to 8 characters': '密码须至少8位',
        'username must match ^[a-zA-Z0-9_]{3,20}$': '用户名须为3-20位字母、数字或下划线',
        'email must be an email': '邮箱格式不正确',
        'displayName must be shorter than or equal to 20 characters': '姓名不能超过20个字符',
        'scheduleType must be a valid enum value': '排班类型无效，须为 INTERVAL 或 FIXED_TIME',
        'role must be a valid enum value': '角色类型无效',
    };
    catch(exception, host) {
        const ctx = host.switchToHttp();
        const response = ctx.getResponse();
        if (exception instanceof common_1.HttpException) {
            const status = exception.getStatus();
            const raw = exception.getResponse();
            const rawMsg = typeof raw === 'string' ? raw : raw.message;
            let message;
            if (Array.isArray(rawMsg)) {
                const translated = rawMsg.map((m) => ChineseExceptionFilter_1.TRANSLATIONS[m] ?? m);
                message = translated.join('；');
            }
            else if (typeof rawMsg === 'string') {
                message = ChineseExceptionFilter_1.TRANSLATIONS[rawMsg] ?? rawMsg;
            }
            else {
                message = String(rawMsg);
            }
            response.status(status).json({ code: status, message });
        }
        else {
            this.logger.error(`Unexpected error: ${exception?.message}`, exception?.stack);
            response.status(common_1.HttpStatus.INTERNAL_SERVER_ERROR).json({
                code: 500,
                message: '服务器内部错误，请稍后重试',
            });
        }
    }
};
exports.ChineseExceptionFilter = ChineseExceptionFilter;
exports.ChineseExceptionFilter = ChineseExceptionFilter = ChineseExceptionFilter_1 = __decorate([
    (0, common_1.Catch)()
], ChineseExceptionFilter);
//# sourceMappingURL=chinese-exception.filter.js.map