import { GroupsService } from './groups.service';
import type { JwtPayload } from '../../common/decorators/current-user.decorator';
import { Role } from '../../../generated/prisma/client';
import { CreateGroupDto, UpdateGroupDto, JoinGroupDto, UpdateMemberStrategyDto, LockStrategyDto } from './dto/group.dto';
export declare class GroupsController {
    private readonly groupsService;
    constructor(groupsService: GroupsService);
    create(user: JwtPayload, dto: CreateGroupDto): Promise<{
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
    getMyGroups(user: JwtPayload): Promise<({
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
    getById(user: JwtPayload, id: string): Promise<{
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
    update(user: JwtPayload, id: string, dto: UpdateGroupDto): Promise<{
        id: string;
        createdAt: Date;
        updatedAt: Date;
        name: string;
        inviteCode: string;
        createdBy: string;
    }>;
    remove(user: JwtPayload, id: string): Promise<{
        id: string;
        createdAt: Date;
        updatedAt: Date;
        name: string;
        inviteCode: string;
        createdBy: string;
    }>;
    regenerateInvite(user: JwtPayload, id: string): Promise<{
        id: string;
        createdAt: Date;
        updatedAt: Date;
        name: string;
        inviteCode: string;
        createdBy: string;
    }>;
    joinByInvite(user: JwtPayload, dto: JoinGroupDto): Promise<{
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
    getMembers(user: JwtPayload, id: string): Promise<{
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
    leave(user: JwtPayload, id: string): Promise<{
        id: string;
        groupId: string;
        userId: string;
        roleInGroup: import("../../../generated/prisma/enums").GroupMemberRole;
        joinedAt: Date;
    }>;
    removeMember(user: JwtPayload, id: string, memberId: string): Promise<{
        id: string;
        groupId: string;
        userId: string;
        roleInGroup: import("../../../generated/prisma/enums").GroupMemberRole;
        joinedAt: Date;
    }>;
    getMemberAlerts(user: JwtPayload, id: string, memberId: string): Promise<{
        id: string;
        createdAt: Date;
        userId: string;
        type: import("../../../generated/prisma/enums").AlertType;
        message: string;
        isResolved: boolean;
        resolvedAt: Date | null;
    }[]>;
    getMemberEvents(user: JwtPayload, id: string, memberId: string, from?: string, to?: string): Promise<{
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
    getMemberStrategies(user: JwtPayload, id: string, memberId: string): Promise<{
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
    updateMemberStrategies(user: JwtPayload, id: string, memberId: string, dto: UpdateMemberStrategyDto): Promise<{
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
    toggleLock(user: JwtPayload, id: string, memberId: string, dto: LockStrategyDto): Promise<{
        target: string;
        isLocked: boolean;
        scheduleId?: undefined;
    } | {
        target: string;
        scheduleId: string;
        isLocked: boolean;
    }>;
    getMemberCalendar(user: JwtPayload, id: string, memberId: string, year: string, month: string): Promise<{
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
    createMedicationForPatient(user: JwtPayload, id: string, memberId: string, dto: {
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
    joinAsStaff(user: JwtPayload, dto: {
        inviteCode: string;
    }): Promise<{
        id: string;
        createdAt: Date;
        groupId: string;
        userId: string;
        status: string;
    }>;
    getAdminRequests(user: JwtPayload, id: string): Promise<({
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
    approveAdminRequest(user: JwtPayload, id: string, requestId: string): Promise<{
        id: string;
        groupId: string;
        userId: string;
        roleInGroup: import("../../../generated/prisma/enums").GroupMemberRole;
        joinedAt: Date;
    }>;
    rejectAdminRequest(user: JwtPayload, id: string, requestId: string): Promise<{
        id: string;
        createdAt: Date;
        groupId: string;
        userId: string;
        status: string;
    }>;
    createVirtualPatient(user: JwtPayload, id: string, dto: {
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
    proxyBmRecord(user: JwtPayload, id: string, memberId: string, dto: {
        notes?: string;
        recordedAt?: string;
    }): Promise<{
        id: string;
        createdAt: Date;
        userId: string;
        recordedAt: Date;
        notes: string | null;
    }>;
    proxyMedicationRecord(user: JwtPayload, id: string, memberId: string, dto: {
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
