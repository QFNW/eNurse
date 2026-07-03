export declare class CreateBmRecordDto {
    recordedAt: string;
    notes?: string;
}
export declare class QueryBmRecordsDto {
    from?: string;
    to?: string;
    page?: string;
    pageSize?: string;
}
export declare class UpdateBmRecordDto {
    notes?: string;
}
export declare class UpdateBmSettingsDto {
    intervalDays?: number;
    reminderTime?: string;
    isEnabled?: boolean;
}
