import {
  IsString,
  IsOptional,
  IsNumber,
  IsEnum,
  IsArray,
  Min,
  IsBoolean,
} from 'class-validator';
import { ScheduleType } from '../../../../generated/prisma/client';

export class CreateMedicationDto {
  @IsString()
  name: string;

  @IsOptional()
  @IsString()
  dosage?: string;

  @IsOptional()
  @IsString()
  unit?: string;

  @IsOptional()
  @IsString()
  notes?: string;
}

export class UpdateMedicationDto {
  @IsOptional()
  @IsString()
  name?: string;

  @IsOptional()
  @IsString()
  dosage?: string;

  @IsOptional()
  @IsString()
  unit?: string;

  @IsOptional()
  @IsString()
  notes?: string;

  @IsOptional()
  @IsBoolean()
  isActive?: boolean;
}

export class CreateScheduleDto {
  @IsEnum(ScheduleType)
  scheduleType: ScheduleType;

  @IsOptional()
  @IsNumber()
  @Min(0)
  intervalHours?: number;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  fixedTimes?: string[];

  @IsOptional()
  @IsString()
  startTime?: string;
}

export class UpdateScheduleDto {
  @IsOptional()
  @IsNumber()
  @Min(0)
  intervalHours?: number;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  fixedTimes?: string[];

  @IsOptional()
  @IsString()
  startTime?: string;

  @IsOptional()
  @IsBoolean()
  isEnabled?: boolean;
}

export class CreateMedicationRecordDto {
  @IsString()
  medicationId: string;

  @IsString()
  takenAt: string;

  @IsOptional()
  @IsString()
  notes?: string;
}
