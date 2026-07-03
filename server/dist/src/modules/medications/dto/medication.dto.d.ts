import { ScheduleType } from '../../../../generated/prisma/client';
export declare class CreateMedicationDto {
    name: string;
    dosage?: string;
    unit?: string;
    notes?: string;
}
export declare class UpdateMedicationDto {
    name?: string;
    dosage?: string;
    unit?: string;
    notes?: string;
    isActive?: boolean;
}
export declare class CreateScheduleDto {
    scheduleType: ScheduleType;
    intervalHours?: number;
    fixedTimes?: string[];
    startTime?: string;
}
export declare class UpdateScheduleDto {
    intervalHours?: number;
    fixedTimes?: string[];
    startTime?: string;
    isEnabled?: boolean;
}
export declare class CreateMedicationRecordDto {
    medicationId: string;
    takenAt: string;
}
