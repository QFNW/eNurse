import { IsString, IsOptional, IsBoolean } from 'class-validator';

export class CreateGroupDto {
  @IsString()
  name: string;
}

export class UpdateGroupDto {
  @IsOptional()
  @IsString()
  name?: string;
}

export class JoinGroupDto {
  @IsString()
  inviteCode: string;
}

export class UpdateMemberStrategyDto {
  // BM settings
  @IsOptional()
  intervalDays?: number;

  @IsOptional()
  reminderTime?: string;

  @IsOptional()
  isEnabled?: boolean;

  // Medication schedule
  @IsOptional()
  scheduleId?: string;

  @IsOptional()
  scheduleType?: string;

  @IsOptional()
  intervalHours?: number;

  @IsOptional()
  fixedTimes?: string[];

  @IsOptional()
  startTime?: string;
}

export class LockStrategyDto {
  @IsString()
  target: 'bm' | 'medication_schedule';

  @IsOptional()
  @IsString()
  scheduleId?: string; // required when target = 'medication_schedule'
}
