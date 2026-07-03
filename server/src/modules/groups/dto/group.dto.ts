import { IsString, IsOptional, IsBoolean, IsInt, IsArray, Min, Matches } from 'class-validator';

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
  @IsOptional()
  @IsInt()
  @Min(1)
  intervalDays?: number;

  @IsOptional()
  @IsString()
  @Matches(/^\d{2}:\d{2}$/)
  reminderTime?: string;

  @IsOptional()
  @IsBoolean()
  isEnabled?: boolean;

  @IsOptional()
  @IsString()
  scheduleId?: string;

  @IsOptional()
  @IsString()
  scheduleType?: string;

  @IsOptional()
  @IsInt()
  @Min(1)
  intervalHours?: number;

  @IsOptional()
  @IsArray()
  fixedTimes?: string[];

  @IsOptional()
  @IsString()
  startTime?: string;
}

export class LockStrategyDto {
  @IsString()
  target: 'bm' | 'medication_schedule';

  @IsOptional()
  @IsString()
  scheduleId?: string;
}
