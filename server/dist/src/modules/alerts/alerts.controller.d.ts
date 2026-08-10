import { AlertsService } from './alerts.service';
import type { JwtPayload } from '../../common/decorators/current-user.decorator';
export declare class AlertsController {
    private readonly alertsService;
    constructor(alertsService: AlertsService);
    findByUser(user: JwtPayload, filter?: string): Promise<{
        id: string;
        createdAt: Date;
        userId: string;
        type: import("../../../generated/prisma/enums").AlertType;
        relatedId: string | null;
        message: string;
        isResolved: boolean;
        resolvedAt: Date | null;
    }[]>;
    resolve(user: JwtPayload, alertId: string): Promise<{
        id: string;
        createdAt: Date;
        userId: string;
        type: import("../../../generated/prisma/enums").AlertType;
        relatedId: string | null;
        message: string;
        isResolved: boolean;
        resolvedAt: Date | null;
    }>;
}
