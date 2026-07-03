import { PrismaService } from '../../prisma/prisma.service';
import { CreateBmRecordDto, QueryBmRecordsDto, UpdateBmRecordDto, UpdateBmSettingsDto } from './dto/bm.dto';
export declare class BmService {
    private prisma;
    constructor(prisma: PrismaService);
    createRecord(userId: string, dto: CreateBmRecordDto): Promise<{
        id: string;
        createdAt: Date;
        userId: string;
        recordedAt: Date;
        notes: string | null;
    }>;
    getRecords(userId: string, query: QueryBmRecordsDto): Promise<{
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
    getRecordById(userId: string, id: string): Promise<{
        id: string;
        createdAt: Date;
        userId: string;
        recordedAt: Date;
        notes: string | null;
    }>;
    updateRecord(userId: string, id: string, dto: UpdateBmRecordDto): Promise<{
        id: string;
        createdAt: Date;
        userId: string;
        recordedAt: Date;
        notes: string | null;
    }>;
    deleteRecord(userId: string, id: string): Promise<{
        id: string;
        createdAt: Date;
        userId: string;
        recordedAt: Date;
        notes: string | null;
    }>;
    getCalendar(userId: string, year: number, month: number, tzOffsetMinutes?: number): Promise<{
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
    getSettings(userId: string): Promise<{
        id: string;
        updatedAt: Date;
        userId: string;
        intervalDays: number;
        reminderTime: string;
        isEnabled: boolean;
        isLocked: boolean;
    }>;
    updateSettings(userId: string, dto: UpdateBmSettingsDto): Promise<{
        id: string;
        updatedAt: Date;
        userId: string;
        intervalDays: number;
        reminderTime: string;
        isEnabled: boolean;
        isLocked: boolean;
    }>;
}
