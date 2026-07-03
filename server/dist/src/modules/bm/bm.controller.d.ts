import { BmService } from './bm.service';
import type { JwtPayload } from '../../common/decorators/current-user.decorator';
import { CreateBmRecordDto, UpdateBmRecordDto, UpdateBmSettingsDto } from './dto/bm.dto';
export declare class BmController {
    private readonly bmService;
    constructor(bmService: BmService);
    createRecord(user: JwtPayload, dto: CreateBmRecordDto): Promise<{
        id: string;
        createdAt: Date;
        userId: string;
        recordedAt: Date;
        notes: string | null;
    }>;
    getRecords(user: JwtPayload, from?: string, to?: string, page?: string, pageSize?: string): Promise<{
        records: {
            id: string;
            createdAt: Date;
            userId: string;
            recordedAt: Date;
            notes: string | null;
        }[];
        total: number;
        page: number;
        pageSize: number;
    }>;
    getRecordById(user: JwtPayload, id: string): Promise<{
        id: string;
        createdAt: Date;
        userId: string;
        recordedAt: Date;
        notes: string | null;
    }>;
    updateRecord(user: JwtPayload, id: string, dto: UpdateBmRecordDto): Promise<{
        id: string;
        createdAt: Date;
        userId: string;
        recordedAt: Date;
        notes: string | null;
    }>;
    deleteRecord(user: JwtPayload, id: string): Promise<{
        id: string;
        createdAt: Date;
        userId: string;
        recordedAt: Date;
        notes: string | null;
    }>;
    getCalendar(user: JwtPayload, year: string, month: string): Promise<{
        year: number;
        month: number;
        days: {
            date: string;
            hasRecord: boolean;
            records: {
                id: string;
                createdAt: Date;
                userId: string;
                recordedAt: Date;
                notes: string | null;
            }[];
        }[];
    }>;
    getSettings(user: JwtPayload): Promise<{
        id: string;
        updatedAt: Date;
        userId: string;
        intervalDays: number;
        reminderTime: string;
        isEnabled: boolean;
        isLocked: boolean;
    }>;
    updateSettings(user: JwtPayload, dto: UpdateBmSettingsDto): Promise<{
        id: string;
        updatedAt: Date;
        userId: string;
        intervalDays: number;
        reminderTime: string;
        isEnabled: boolean;
        isLocked: boolean;
    }>;
}
