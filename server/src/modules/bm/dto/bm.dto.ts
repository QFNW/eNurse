import { IsInt, IsString, IsOptional, Min, Max } from 'class-validator';

export class CreateBmRecordDto {
  @IsString()
  recordedAt: string; // ISO 8601

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
  isEnabled?: boolean;
}
