import { Injectable } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';

@Injectable()
export class UsersService {
  constructor(private prisma: PrismaService) {}

  async findById(id: string) {
    return this.prisma.user.findUnique({
      where: { id },
      select: { id: true, email: true, nickname: true, role: true, createdAt: true },
    });
  }

  async update(id: string, data: { nickname?: string }) {
    return this.prisma.user.update({
      where: { id },
      data,
      select: { id: true, email: true, nickname: true, role: true },
    });
  }
}
