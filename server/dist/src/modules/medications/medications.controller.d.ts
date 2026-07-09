import { MedicationsService } from './medications.service';
import type { JwtPayload } from '../../common/decorators/current-user.decorator';
import { CreateMedicationDto, UpdateMedicationDto, CreateScheduleDto, UpdateScheduleDto, CreateMedicationRecordDto } from './dto/medication.dto';
export declare class MedicationsController {
    private readonly medicationsService;
    constructor(medicationsService: MedicationsService);
    create(user: JwtPayload, dto: CreateMedicationDto): Promise<{
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
    findAll(user: JwtPayload): Promise<({
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
    createRecord(user: JwtPayload, dto: CreateMedicationRecordDto): Promise<{
        id: string;
        createdAt: Date;
        userId: string;
        notes: string | null;
        medicationId: string;
        takenAt: Date;
    }>;
    getRecords(user: JwtPayload, from?: string, to?: string): Promise<({
        medication: {
            id: string;
            name: string;
        };
    } & {
        id: string;
        createdAt: Date;
        userId: string;
        notes: string | null;
        medicationId: string;
        takenAt: Date;
    })[]>;
    findById(user: JwtPayload, id: string): Promise<{
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
    update(user: JwtPayload, id: string, dto: UpdateMedicationDto): Promise<{
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
    remove(user: JwtPayload, id: string): Promise<{
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
    createSchedule(user: JwtPayload, medicationId: string, dto: CreateScheduleDto): Promise<{
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
    updateSchedule(user: JwtPayload, scheduleId: string, dto: UpdateScheduleDto): Promise<{
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
    deleteSchedule(user: JwtPayload, scheduleId: string): Promise<{
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
}
