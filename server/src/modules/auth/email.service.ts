import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { CacheService } from '../cache/cache.service';
import * as nodemailer from 'nodemailer';

@Injectable()
export class EmailService {
  private transporter: nodemailer.Transporter | null = null;

  constructor(
    private configService: ConfigService,
    private cacheService: CacheService,
  ) {
    const user = this.configService.get<string>('smtp.user');
    const pass = this.configService.get<string>('smtp.pass');
    if (user && pass) {
      const port = this.configService.get<number>('smtp.port') || 465;
      this.transporter = nodemailer.createTransport({
        host: this.configService.get<string>('smtp.host'),
        port,
        secure: port === 465,
        auth: { user, pass },
      });
    } else {
      console.warn('SMTP not configured — verification codes will be logged to console');
    }
  }

  async sendVerificationCode(email: string): Promise<void> {
    const code = String(Math.floor(100000 + Math.random() * 900000));

    // Store in Redis with 5-minute TTL
    await this.cacheService.setCode(email, code, 300);

    if (this.transporter) {
      await this.transporter.sendMail({
        from: this.configService.get<string>('smtp.user'),
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
    } else {
      console.log(`[EmailService] 验证码 for ${email}: ${code}`);
    }
  }

  /** Generic mail send — falls back to console log when SMTP is not configured */
  async sendMail(to: string, subject: string, html: string): Promise<void> {
    if (this.transporter && to) {
      await this.transporter.sendMail({
        from: this.configService.get<string>('smtp.user'),
        to,
        subject,
        html,
      });
    } else {
      console.log(`[EmailService] ${subject} -> ${to}: ${html}`);
    }
  }

  async verifyCode(email: string, code: string): Promise<boolean> {
    const stored = await this.cacheService.getCode(email);
    return stored !== null && stored === code;
  }

  async consumeCode(email: string, code: string): Promise<boolean> {
    const valid = await this.verifyCode(email, code);
    if (valid) {
      await this.cacheService.deleteCode(email);
    }
    return valid;
  }
}
