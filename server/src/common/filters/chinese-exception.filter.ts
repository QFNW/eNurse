import { ExceptionFilter, Catch, ArgumentsHost, HttpException, HttpStatus, Logger } from '@nestjs/common';
import { Response } from 'express';

@Catch()
export class ChineseExceptionFilter implements ExceptionFilter {
  private readonly logger = new Logger(ChineseExceptionFilter.name);

  private static readonly TRANSLATIONS: Record<string, string> = {
    'intervalHours must not be less than 0': '用药间隔须为正数',
    'intervalDays must not be less than 1': '大便间隔天数须 ≥ 1',
    'password must be longer than or equal to 8 characters': '密码须至少8位',
    'username must match ^[a-zA-Z0-9_]{3,20}$': '用户名须为3-20位字母、数字或下划线',
    'email must be an email': '邮箱格式不正确',
    'displayName must be shorter than or equal to 20 characters': '姓名不能超过20个字符',
    'scheduleType must be a valid enum value': '排班类型无效，须为 INTERVAL 或 FIXED_TIME',
    'role must be a valid enum value': '角色类型无效',
  };

  catch(exception: unknown, host: ArgumentsHost) {
    const ctx = host.switchToHttp();
    const response = ctx.getResponse<Response>();

    if (exception instanceof HttpException) {
      const status = exception.getStatus();
      const raw = exception.getResponse();
      const rawMsg: any = typeof raw === 'string' ? raw : (raw as any).message;

      let message: string;
      if (Array.isArray(rawMsg)) {
        // class-validator errors
        const translated = rawMsg.map((m: string) =>
          ChineseExceptionFilter.TRANSLATIONS[m] ?? m
        );
        message = translated.join('；');
      } else if (typeof rawMsg === 'string') {
        message = ChineseExceptionFilter.TRANSLATIONS[rawMsg] ?? rawMsg;
      } else {
        message = String(rawMsg);
      }

      response.status(status).json({ code: status, message });
    } else {
      this.logger.error(`Unexpected error: ${(exception as Error)?.message}`, (exception as Error)?.stack);
      response.status(HttpStatus.INTERNAL_SERVER_ERROR).json({
        code: 500,
        message: '服务器内部错误，请稍后重试',
      });
    }
  }
}
