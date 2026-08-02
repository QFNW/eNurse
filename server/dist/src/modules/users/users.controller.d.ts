import { UsersService } from './users.service';
import type { JwtPayload } from '../../common/decorators/current-user.decorator';
declare class UpdateProfileDto {
    nickname?: string;
}
export declare class UsersController {
    private readonly usersService;
    constructor(usersService: UsersService);
    getProfile(user: JwtPayload): Promise<{
        id: string;
        email: string;
        nickname: string | null;
        role: import("../../../generated/prisma/enums").Role;
        createdAt: Date;
    } | null>;
    updateProfile(user: JwtPayload, body: UpdateProfileDto): Promise<{
        id: string;
        email: string;
        nickname: string | null;
        role: import("../../../generated/prisma/enums").Role;
    }>;
    deleteAccount(user: JwtPayload): Promise<{
        message: string;
    }>;
}
export {};
