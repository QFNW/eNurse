export declare class CreateGroupDto {
    name: string;
}
export declare class UpdateGroupDto {
    name?: string;
}
export declare class JoinGroupDto {
    inviteCode: string;
}
export declare class UpdateMemberStrategyDto {
    intervalDays?: number;
    reminderTime?: string;
    isEnabled?: boolean;
    scheduleId?: string;
    scheduleType?: string;
    intervalHours?: number;
    fixedTimes?: string[];
    startTime?: string;
}
export declare class LockStrategyDto {
    target: 'bm' | 'medication_schedule';
    scheduleId?: string;
}
