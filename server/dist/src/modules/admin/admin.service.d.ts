import { ConfigService } from '@nestjs/config';
import { JwtService } from '@nestjs/jwt';
import { PrismaService } from '../../prisma/prisma.service';
import { Role } from '../../../generated/prisma/client';
export declare class AdminService {
    private prisma;
    private jwtService;
    private configService;
    constructor(prisma: PrismaService, jwtService: JwtService, configService: ConfigService);
    login(password: string): Promise<{
        accessToken: string;
    }>;
    getUsers(search?: string, page?: number, pageSize?: number): Promise<{
        users: {
            id: string;
            email: string;
            nickname: string | null;
            role: Role;
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
    updateUserRole(userId: string, role: string): Promise<{
        id: string;
        email: string;
        nickname: string | null;
        role: Role;
    }>;
    disableUser(userId: string, requesterId?: string): Promise<{
        id: string;
        email: string;
        role: Role;
    }>;
    getGroups(page?: number, pageSize?: number): Promise<{
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
