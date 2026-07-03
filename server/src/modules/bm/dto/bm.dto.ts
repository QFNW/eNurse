import { IsInt, IsString, IsOptional, IsBoolean, Min, Max, Matches } from 'class-validator';

export class CreateBmRecordDto {
  @Matches(/^\d{4}-\d{2}-\d{2}(T\d{2}:\d{2}(:\d{2})?)?$/)
  recordedAt: string; // ISO 8601 date or datetime

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
