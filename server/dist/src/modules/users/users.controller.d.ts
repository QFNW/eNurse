import { UsersService } from './users.service';
import type { JwtPayload } from '../../common/decorators/current-user.decorator';
declare class UpdateProfileDto {
    nickname?: string;
    displayName?: string;
}
declare class ChangePasswordDto {
    oldPassword: string;
    newPassword: string;
}
export declare class UsersController {
    private readonly usersService;
    constructor(usersService: UsersService);
    getProfile(user: JwtPayload): Promise<{
        id: string;
        email: string;
        displayName: string;
        nickname: string | null;
        role: import("../../../generated/prisma/enums").Role;
        createdAt: Date;
    } | null>;
    updateProfile(user: JwtPayload, body: UpdateProfileDto): Promise<{
        id: string;
        email: string;
        displayName: string;
        nickname: string | null;
        role: import("../../../generated/prisma/enums").Role;
    }>;
    changePassword(user: JwtPayload, body: ChangePasswordDto): Promise<{
        message: string;
    }>;
    deleteAccount(user: JwtPayload): Promise<{
        message: string;
    }>;
}
export {};
