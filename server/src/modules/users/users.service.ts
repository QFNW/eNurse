import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import * as bcrypt from 'bcrypt';
import { PrismaService } from '../../prisma/prisma.service';

@Injectable()
export class UsersService {
  constructor(private prisma: PrismaService) {}

  async findById(id: string) {
    return this.prisma.user.findUnique({
      where: { id },
      select: {
        id: true,
        email: true,
        nickname: true,
        displayName: true,
        role: true,
        createdAt: true,
      },
    });
  }

  async update(id: string, data: { nickname?: string; displayName?: string }) {
    return this.prisma.user.update({
      where: { id },
      data,
      select: {
        id: true,
        email: true,
        nickname: true,
        displayName: true,
        role: true,
      },
    });
  }

  async changePassword(id: string, oldPassword: string, newPassword: string) {
    const user = await this.prisma.user.findUnique({ where: { id } });
    if (!user) throw new NotFoundException('用户不存在');

    const valid = await bcrypt.compare(oldPassword, user.passwordHash);
    if (!valid) throw new BadRequestException('原密码错误');

    const passwordHash = await bcrypt.hash(newPassword, 12);
    await this.prisma.user.update({
      where: { id },
      data: { passwordHash },
    });
    return { message: '密码已修改，请重新登录' };
  }

  async delete(id: string) {
    await this.prisma.user.delete({ where: { id } });
    return { message: '账号已注销' };
  }
}
