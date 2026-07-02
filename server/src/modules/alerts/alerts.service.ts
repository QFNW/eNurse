import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';

@Injectable()
export class AlertsService {
  constructor(private prisma: PrismaService) {}

  async findByUser(userId: string, filter?: string) {
    const where: any = { userId };
    if (filter === 'active') {
      where.isResolved = false;
    }

    return this.prisma.alert.findMany({
      where,
      orderBy: { createdAt: 'desc' },
      take: 50,
    });
  }

  async resolve(userId: string, alertId: string) {
    const alert = await this.prisma.alert.findUnique({
      where: { id: alertId },
    });
    if (!alert || alert.userId !== userId) {
      throw new NotFoundException('警报不存在');
    }

    return this.prisma.alert.update({
      where: { id: alertId },
      data: { isResolved: true, resolvedAt: new Date() },
    });
  }

  /** Get active alerts count for a user (used by nurse dashboard) */
  async getActiveCount(userId: string) {
    return this.prisma.alert.count({
      where: { userId, isResolved: false },
    });
  }
}
