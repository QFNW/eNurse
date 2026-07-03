import { PrismaService } from '../../prisma/prisma.service';
export declare class AlertsService {
    private prisma;
    constructor(prisma: PrismaService);
    findByUser(userId: string, filter?: string): Promise<{
        id: string;
        createdAt: Date;
        userId: string;
        type: import("../../../generated/prisma/enums").AlertType;
        message: string;
        isResolved: boolean;
        resolvedAt: Date | null;
    }[]>;
    resolve(userId: string, alertId: string): Promise<{
        id: string;
        createdAt: Date;
        userId: string;
        type: import("../../../generated/prisma/enums").AlertType;
        message: string;
        isResolved: boolean;
        resolvedAt: Date | null;
    }>;
    getActiveCount(userId: string): Promise<number>;
}
