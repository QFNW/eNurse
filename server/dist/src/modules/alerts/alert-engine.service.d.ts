import { PrismaService } from '../../prisma/prisma.service';
export declare class AlertEngineService {
    private prisma;
    private readonly logger;
    constructor(prisma: PrismaService);
    checkBmReminders(): Promise<void>;
    private checkSingleBmReminder;
    checkMedicationReminders(): Promise<void>;
    private checkSingleMedicationReminder;
    cleanupStaleAlerts(): Promise<void>;
    private resolvePreviousAlerts;
    private notifyGroupNurses;
    private parseFixedTimes;
    private isTimeMatch;
}
