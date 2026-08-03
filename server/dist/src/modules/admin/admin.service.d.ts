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
            isDisabled: boolean;
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
    setUserStatus(userId: string, isDisabled: boolean, requesterId?: string): Promise<{
        id: string;
        email: string;
        nickname: string | null;
        role: Role;
        isDisabled: boolean;
    }>;
    resetPassword(userId: string, newPassword: string): Promise<{
        message: string;
    }>;
    deleteUser(userId: string): Promise<{
        message: string;
    }>;
    getGroupMembers(groupId: string): Promise<{
        group: {
            id: string;
            name: string;
        };
        members: {
            user: {
                displayName: string;
                id: string;
                email: string;
                nickname: string | null;
                role: Role;
            } | null;
            id: string;
            roleInGroup: import("../../../generated/prisma/enums").GroupMemberRole;
            joinedAt: Date;
        }[];
    }>;
    deleteGroup(groupId: string): Promise<{
        message: string;
    }>;
    getUserDetail(userId: string): Promise<{
        groups: {
            id: string;
            name: string;
            roleInGroup: import("../../../generated/prisma/enums").GroupMemberRole;
            joinedAt: Date;
            memberCount: number;
        }[];
        bmRecords: {
            id: string;
            createdAt: Date;
            userId: string;
            recordedAt: Date;
            notes: string | null;
        }[];
        medicationRecords: ({
            medication: {
                id: string;
                name: string;
            };
        } & {
            id: string;
            createdAt: Date;
            userId: string;
            notes: string | null;
            medicationId: string;
            takenAt: Date;
        })[];
        medications: {
            id: string;
            createdAt: Date;
            updatedAt: Date;
            name: string;
            userId: string;
            notes: string | null;
            dosage: string | null;
            unit: string | null;
            isActive: boolean;
        }[];
        counts: {
            bmRecords: number;
            medicationRecords: number;
            groupMemberships: number;
        };
        id: string;
        username: string;
        email: string;
        displayName: string;
        nickname: string | null;
        role: Role;
        isVirtual: boolean;
        isDisabled: boolean;
        createdAt: Date;
        updatedAt: Date;
        _count: {
            bmRecords: number;
            medicationRecords: number;
            groupMemberships: number;
        };
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
    getFeedback(page?: number, pageSize?: number): Promise<{
        feedbacks: ({
            user: {
                email: string;
                displayName: string;
                nickname: string | null;
            } | null;
        } & {
            id: string;
            createdAt: Date;
            userId: string | null;
            content: string;
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
