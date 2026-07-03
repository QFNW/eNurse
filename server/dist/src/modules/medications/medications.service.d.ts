import { PrismaService } from '../../prisma/prisma.service';
import { CreateMedicationDto, UpdateMedicationDto, CreateScheduleDto, UpdateScheduleDto, CreateMedicationRecordDto } from './dto/medication.dto';
export declare class MedicationsService {
    private prisma;
    constructor(prisma: PrismaService);
    create(userId: string, dto: CreateMedicationDto): Promise<{
        id: string;
        createdAt: Date;
        updatedAt: Date;
        name: string;
        userId: string;
        notes: string | null;
        dosage: string | null;
        unit: string | null;
        isActive: boolean;
    }>;
    findAll(userId: string): Promise<({
        schedules: {
            id: string;
            updatedAt: Date;
            isEnabled: boolean;
            isLocked: boolean;
            medicationId: string;
            scheduleType: import("../../../generated/prisma/enums").ScheduleType;
            intervalHours: number | null;
            fixedTimes: import("@prisma/client/runtime/client").JsonValue | null;
            startTime: Date | null;
        }[];
    } & {
        id: string;
        createdAt: Date;
        updatedAt: Date;
        name: string;
        userId: string;
        notes: string | null;
        dosage: string | null;
        unit: string | null;
        isActive: boolean;
    })[]>;
    findById(userId: string, id: string): Promise<{
        schedules: {
            id: string;
            updatedAt: Date;
            isEnabled: boolean;
            isLocked: boolean;
            medicationId: string;
            scheduleType: import("../../../generated/prisma/enums").ScheduleType;
            intervalHours: number | null;
            fixedTimes: import("@prisma/client/runtime/client").JsonValue | null;
            startTime: Date | null;
        }[];
    } & {
        id: string;
        createdAt: Date;
        updatedAt: Date;
        name: string;
        userId: string;
        notes: string | null;
        dosage: string | null;
        unit: string | null;
        isActive: boolean;
    }>;
    update(userId: string, id: string, dto: UpdateMedicationDto): Promise<{
        id: string;
        createdAt: Date;
        updatedAt: Date;
        name: string;
        userId: string;
        notes: string | null;
        dosage: string | null;
        unit: string | null;
        isActive: boolean;
    }>;
    remove(userId: string, id: string): Promise<{
        id: string;
        createdAt: Date;
        updatedAt: Date;
        name: string;
        userId: string;
        notes: string | null;
        dosage: string | null;
        unit: string | null;
        isActive: boolean;
    }>;
    createSchedule(userId: string, medicationId: string, dto: CreateScheduleDto): Promise<{
        id: string;
        updatedAt: Date;
        isEnabled: boolean;
        isLocked: boolean;
        medicationId: string;
        scheduleType: import("../../../generated/prisma/enums").ScheduleType;
        intervalHours: number | null;
        fixedTimes: import("@prisma/client/runtime/client").JsonValue | null;
        startTime: Date | null;
    }>;
    updateSchedule(userId: string, scheduleId: string, dto: UpdateScheduleDto): Promise<{
        id: string;
        updatedAt: Date;
        isEnabled: boolean;
        isLocked: boolean;
        medicationId: string;
        scheduleType: import("../../../generated/prisma/enums").ScheduleType;
        intervalHours: number | null;
        fixedTimes: import("@prisma/client/runtime/client").JsonValue | null;
        startTime: Date | null;
    }>;
    deleteSchedule(userId: string, scheduleId: string): Promise<{
        id: string;
        updatedAt: Date;
        isEnabled: boolean;
        isLocked: boolean;
        medicationId: string;
        scheduleType: import("../../../generated/prisma/enums").ScheduleType;
        intervalHours: number | null;
        fixedTimes: import("@prisma/client/runtime/client").JsonValue | null;
        startTime: Date | null;
    }>;
    createRecord(userId: string, dto: CreateMedicationRecordDto): Promise<{
        id: string;
        createdAt: Date;
        userId: string;
        medicationId: string;
        takenAt: Date;
    }>;
    getRecords(userId: string, from?: string, to?: string): Promise<({
        medication: {
            id: string;
            name: string;
        };
    } & {
        id: string;
        createdAt: Date;
        userId: string;
        medicationId: string;
        takenAt: Date;
    })[]>;
}
