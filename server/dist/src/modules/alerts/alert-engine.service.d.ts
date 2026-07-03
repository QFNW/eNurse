import type { Queue } from 'bull';
import { PrismaService } from '../../prisma/prisma.service';
export declare class AlertEngineService {
    private prisma;
    private alertsQueue;
    private readonly logger;
    constructor(prisma: PrismaService, alertsQueue: Queue);
    checkBmReminders(): Promise<void>;
    private checkSingleBmReminder;
    checkMedicationReminders(): Promise<void>;
    private checkSingleMedicationReminder;
    private enqueueAlert;
    private parseFixedTimes;
    private isTimeMatch;
}
