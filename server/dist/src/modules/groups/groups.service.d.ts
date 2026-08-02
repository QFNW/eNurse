import { PrismaService } from '../../prisma/prisma.service';
import { Role } from '../../../generated/prisma/client';
import { CreateGroupDto, UpdateGroupDto, JoinGroupDto, UpdateMemberStrategyDto } from './dto/group.dto';
export declare class GroupsService {
    private prisma;
    constructor(prisma: PrismaService);
    create(userId: string, dto: CreateGroupDto): Promise<{
        members: ({
            user: {
                id: string;
                email: string;
                nickname: string | null;
                role: Role;
            };
        } & {
            id: string;
            groupId: string;
            userId: string;
            roleInGroup: import("../../../generated/prisma/enums").GroupMemberRole;
            joinedAt: Date;
        })[];
    } & {
        id: string;
        createdAt: Date;
        updatedAt: Date;
        name: string;
        inviteCode: string;
        createdBy: string;
    }>;
    findByUser(userId: string, role: string): Promise<({
        _count: {
            members: number;
        };
    } & {
        id: string;
        createdAt: Date;
        updatedAt: Date;
        name: string;
        inviteCode: string;
        createdBy: string;
    })[]>;
    findById(userId: string, groupId: string): Promise<{
        _count: {
            members: number;
        };
        members: ({
            user: {
                id: string;
                username: string;
                email: string;
                displayName: string;
                nickname: string | null;
                role: Role;
            };
        } & {
            id: string;
            groupId: string;
            userId: string;
            roleInGroup: import("../../../generated/prisma/enums").GroupMemberRole;
            joinedAt: Date;
        })[];
        id: string;
        createdAt: Date;
        updatedAt: Date;
        name: string;
        createdBy: string;
    }>;
    update(userId: string, groupId: string, dto: UpdateGroupDto): Promise<{
        id: string;
        createdAt: Date;
        updatedAt: Date;
        name: string;
        inviteCode: string;
        createdBy: string;
    }>;
    remove(userId: string, groupId: string): Promise<{
        id: string;
        createdAt: Date;
        updatedAt: Date;
        name: string;
        inviteCode: string;
        createdBy: string;
    }>;
    regenerateInvite(userId: string, groupId: string): Promise<{
        id: string;
        createdAt: Date;
        updatedAt: Date;
        name: string;
        inviteCode: string;
        createdBy: string;
    }>;
    joinByInvite(userId: string, dto: JoinGroupDto): Promise<{
        group: {
            id: string;
            createdAt: Date;
            updatedAt: Date;
            name: string;
            inviteCode: string;
            createdBy: string;
        };
    } & {
        id: string;
        groupId: string;
        userId: string;
        roleInGroup: import("../../../generated/prisma/enums").GroupMemberRole;
        joinedAt: Date;
    }>;
    leave(userId: string, groupId: string): Promise<{
        id: string;
        groupId: string;
        userId: string;
        roleInGroup: import("../../../generated/prisma/enums").GroupMemberRole;
        joinedAt: Date;
    }>;
    getMembers(userId: string, groupId: string): Promise<{
        id: string;
        groupId: string;
        userId: string;
        roleInGroup: import("../../../generated/prisma/enums").GroupMemberRole;
        joinedAt: Date;
        user: {
            id: string;
            displayName: string;
            nickname: string | null;
            role: Role;
        };
    }[]>;
    removeMember(ownerId: string, groupId: string, memberId: string): Promise<{
        id: string;
        groupId: string;
        userId: string;
        roleInGroup: import("../../../generated/prisma/enums").GroupMemberRole;
        joinedAt: Date;
    }>;
    getMemberAlerts(ownerId: string, groupId: string, memberId: string): Promise<{
        id: string;
        createdAt: Date;
        userId: string;
        type: import("../../../generated/prisma/enums").AlertType;
        message: string;
        isResolved: boolean;
        resolvedAt: Date | null;
    }[]>;
    getMemberEvents(ownerId: string, groupId: string, memberId: string, from?: string, to?: string): Promise<{
        bmRecords: {
            id: string;
            createdAt: Date;
            userId: string;
            recordedAt: Date;
            notes: string | null;
        }[];
        medRecords: ({
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
        alerts: {
            id: string;
            createdAt: Date;
            userId: string;
            type: import("../../../generated/prisma/enums").AlertType;
            message: string;
            isResolved: boolean;
            resolvedAt: Date | null;
        }[];
    }>;
    getMemberStrategies(ownerId: string, groupId: string, memberId: string): Promise<{
        bmSettings: {
            id: string;
            updatedAt: Date;
            userId: string;
            intervalDays: number;
            reminderTime: string;
            isEnabled: boolean;
            isLocked: boolean;
        } | null;
        medications: ({
            schedules: {
                id: string;
                updatedAt: Date;
                isEnabled: boolean;
                isLocked: boolean;
                medicationId: string;
                scheduleType: import("../../../generated/prisma/enums").ScheduleType;
                intervalHours: number | null;
                fixedTimes: import("@prisma/client/runtime/client").JsonValue | null;
                startTime: Date | null;
            }[];
        } & {
            id: string;
            createdAt: Date;
            updatedAt: Date;
            name: string;
            userId: string;
            notes: string | null;
            dosage: string | null;
            unit: string | null;
            isActive: boolean;
        })[];
    }>;
    updateMemberStrategies(ownerId: string, groupId: string, memberId: string, dto: UpdateMemberStrategyDto): Promise<{
        bmSettings: {
            id: string;
            updatedAt: Date;
            userId: string;
            intervalDays: number;
            reminderTime: string;
            isEnabled: boolean;
            isLocked: boolean;
        } | null;
        medications: ({
            schedules: {
                id: string;
                updatedAt: Date;
                isEnabled: boolean;
                isLocked: boolean;
                medicationId: string;
                scheduleType: import("../../../generated/prisma/enums").ScheduleType;
                intervalHours: number | null;
                fixedTimes: import("@prisma/client/runtime/client").JsonValue | null;
                startTime: Date | null;
            }[];
        } & {
            id: string;
            createdAt: Date;
            updatedAt: Date;
            name: string;
            userId: string;
            notes: string | null;
            dosage: string | null;
            unit: string | null;
            isActive: boolean;
        })[];
    }>;
    toggleLock(ownerId: string, groupId: string, memberId: string, target: 'bm' | 'medication_schedule', scheduleId?: string): Promise<{
        target: string;
        isLocked: boolean;
        scheduleId?: undefined;
    } | {
        target: string;
        scheduleId: string;
        isLocked: boolean;
    }>;
    getMemberCalendar(ownerId: string, groupId: string, memberId: string, year: number, month: number, tzOffsetMinutes?: number): Promise<{
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
    private ensureOwner;
    private ensureMemberInGroup;
    private ensureAdminOrOwner;
    private generateInviteCode;
    createMedicationForPatient(nurseId: string, groupId: string, patientId: string, dto: {
        name: string;
        dosage?: string;
    }): Promise<{
        id: string;
        createdAt: Date;
        updatedAt: Date;
        name: string;
        userId: string;
        notes: string | null;
        dosage: string | null;
        unit: string | null;
        isActive: boolean;
    }>;
    joinAsStaff(userId: string, inviteCode: string): Promise<{
        id: string;
        createdAt: Date;
        groupId: string;
        userId: string;
        status: string;
    }>;
    getAdminRequests(userId: string, groupId: string): Promise<({
        user: {
            id: string;
            username: string;
            email: string;
            displayName: string;
        };
    } & {
        id: string;
        createdAt: Date;
        groupId: string;
        userId: string;
        status: string;
    })[]>;
    approveAdminRequest(userId: string, groupId: string, requestId: string): Promise<{
        id: string;
        groupId: string;
        userId: string;
        roleInGroup: import("../../../generated/prisma/enums").GroupMemberRole;
        joinedAt: Date;
    }>;
    rejectAdminRequest(userId: string, groupId: string, requestId: string): Promise<{
        id: string;
        createdAt: Date;
        groupId: string;
        userId: string;
        status: string;
    }>;
    createVirtualPatient(nurseId: string, groupId: string, dto: {
        name: string;
        notes?: string;
    }): Promise<{
        id: string;
        username: string;
        email: string;
        passwordHash: string;
        displayName: string;
        nickname: string | null;
        role: Role;
        isVirtual: boolean;
        isDisabled: boolean;
        createdAt: Date;
        updatedAt: Date;
    }>;
    proxyBmRecord(nurseId: string, groupId: string, patientId: string, dto: {
        notes?: string;
        recordedAt?: string;
    }): Promise<{
        id: string;
        createdAt: Date;
        userId: string;
        recordedAt: Date;
        notes: string | null;
    }>;
    proxyMedicationRecord(nurseId: string, groupId: string, patientId: string, dto: {
        medicationId: string;
        notes?: string;
    }): Promise<{
        id: string;
        createdAt: Date;
        userId: string;
        notes: string | null;
        medicationId: string;
        takenAt: Date;
    }>;
}
