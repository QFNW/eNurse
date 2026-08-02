import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';
import { EmailService } from '../auth/email.service';
import { ConfigService } from '@nestjs/config';

@Injectable()
export class FeedbackService {
  constructor(
    private prisma: PrismaService,
    private emailService: EmailService,
    private configService: ConfigService,
  ) {}

  /** Create a feedback record and self-send an email notification to the SMTP account owner */
  async create(userId: string, content: string): Promise<{ message: string }> {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { id: true, email: true, nickname: true, displayName: true },
    });
    if (!user) throw new NotFoundException('用户不存在');

    await this.prisma.feedback.create({
      data: { userId, content },
    });

    const identity = [user.displayName, user.nickname, user.email].filter(Boolean).join(' / ');
    await this.emailService.sendMail(
      this.configService.get<string>('smtp.user') || 'feedback@enurse.local',
      '易护 eNurse - 用户反馈',
      `
        <div style="max-width:480px;margin:0 auto;padding:32px;font-family:Arial,sans-serif">
          <h2 style="color:#1565C0">易护 eNurse - 用户反馈</h2>
          <p><strong>用户：</strong>${identity}</p>
          <p><strong>内容：</strong></p>
          <div style="padding:16px 24px;background:#F5F5F5;border-radius:8px;white-space:pre-wrap">${content}</div>
        </div>
      `,
    );

    return { message: '反馈已提交，感谢您的建议' };
  }
}
