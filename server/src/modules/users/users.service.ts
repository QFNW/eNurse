import { Injectable } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';

@Injectable()
export class UsersService {
  constructor(private prisma: PrismaService) {}

  async findById(id: string) {
    return this.prisma.user.findUnique({
      where: { id },
      select: { id: true, email: true, nickname: true, role: true, wechatWxid: true, createdAt: true },
    });
  }

  async update(id: string, data: { nickname?: string }) {
    return this.prisma.user.update({
      where: { id },
      data,
      select: { id: true, email: true, nickname: true, role: true },
    });
  }

  async bindWechat(id: string, wechatWxid: string) {
    const existing = await this.prisma.user.findUnique({
      where: { wechatWxid },
    });
    if (existing && existing.id !== id) {
      throw new Error('该微信已绑定其他账号');
    }
    return this.prisma.user.update({
      where: { id },
      data: { wechatWxid, wechatBindAt: new Date() },
      select: { id: true, email: true, wechatWxid: true, wechatBindAt: true },
    });
  }
}
