import { UsersService } from './users.service';
import type { JwtPayload } from '../../common/decorators/current-user.decorator';
export declare class UsersController {
    private readonly usersService;
    constructor(usersService: UsersService);
    getProfile(user: JwtPayload): Promise<{
        id: string;
        email: string;
        nickname: string | null;
        role: import("../../../generated/prisma/enums").Role;
        wechatWxid: string | null;
        createdAt: Date;
    } | null>;
    updateProfile(user: JwtPayload, body: {
        nickname?: string;
    }): Promise<{
        id: string;
        email: string;
        nickname: string | null;
        role: import("../../../generated/prisma/enums").Role;
    }>;
    bindWechat(user: JwtPayload, body: {
        wechatWxid: string;
    }): Promise<{
        id: string;
        email: string;
        wechatWxid: string | null;
        wechatBindAt: Date | null;
    }>;
}
