import { AdminService } from './admin.service';
import { Role } from '../../../generated/prisma/client';
export declare class AdminController {
    private readonly adminService;
    constructor(adminService: AdminService);
    login(body: {
        password: string;
    }): Promise<{
        accessToken: string;
    }>;
    getUsers(search?: string, page?: string, pageSize?: string): Promise<{
        users: {
            id: string;
            email: string;
            nickname: string | null;
            role: Role;
            wechatWxid: string | null;
            createdAt: Date;
            _count: {
                bmRecords: number;
                medications: number;
            };
        }[];
        total: number;
        page: number;
        pageSize: number;
    }>;
    updateUserRole(userId: string, body: {
        role: string;
    }): Promise<{
        id: string;
        email: string;
        nickname: string | null;
        role: Role;
    }>;
    disableUser(userId: string): Promise<{
        id: string;
        email: string;
        role: Role;
    }>;
    getGroups(page?: string, pageSize?: string): Promise<{
        groups: ({
            _count: {
                members: number;
            };
            creator: {
                email: string;
                nickname: string | null;
            };
        } & {
            id: string;
            createdAt: Date;
            updatedAt: Date;
            name: string;
            inviteCode: string;
            createdBy: string;
        })[];
        total: number;
        page: number;
        pageSize: number;
    }>;
    getStats(): Promise<{
        users: {
            total: number;
            patients: number;
            nurses: number;
        };
        groups: number;
        records: {
            bmTotal: number;
            bmToday: number;
            medTotal: number;
            medToday: number;
        };
        alerts: {
            active: number;
        };
    }>;
}
