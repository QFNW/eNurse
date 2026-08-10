import { IsInt, IsString, IsOptional, IsBoolean, Min, Max, Matches } from 'class-validator';

export class CreateBmRecordDto {
  // ISO 8601 日期或时间，兼容三种格式：
  // 旧 App：2026-08-10T20:30:00；新 App：2026-08-10T20:30:00.123+08:00；服务端回显：...Z
  @Matches(/^\d{4}-\d{2}-\d{2}(T\d{2}:\d{2}(:\d{2}(\.\d{1,3})?)?(Z|[+-]\d{2}:\d{2})?)?$/)
  recordedAt: string;

  @IsOptional()
  @IsString()
  notes?: string;
}

export class QueryBmRecordsDto {
  @IsOptional()
  @IsString()
  from?: string;

  @IsOptional()
  @IsString()
  to?: string;

  @IsOptional()
  @IsString()
  page?: string;

  @IsOptional()
  @IsString()
  pageSize?: string;
}

export class UpdateBmRecordDto {
  @IsOptional()
  @IsString()
  notes?: string;
}

export class UpdateBmSettingsDto {
  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(30)
  intervalDays?: number;

  @IsOptional()
  @IsString()
  reminderTime?: string;

  @IsOptional()
  @IsBoolean()
  isEnabled?: boolean;
}
