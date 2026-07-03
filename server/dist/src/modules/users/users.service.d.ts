import { PrismaService } from '../../prisma/prisma.service';
export declare class UsersService {
    private prisma;
    constructor(prisma: PrismaService);
    findById(id: string): Promise<{
        id: string;
        email: string;
        nickname: string | null;
        role: import("../../../generated/prisma/enums").Role;
        wechatWxid: string | null;
        createdAt: Date;
    } | null>;
    update(id: string, data: {
        nickname?: string;
    }): Promise<{
        id: string;
        email: string;
        nickname: string | null;
        role: import("../../../generated/prisma/enums").Role;
    }>;
    bindWechat(id: string, wechatWxid: string): Promise<{
        id: string;
        email: string;
        wechatWxid: string | null;
        wechatBindAt: Date | null;
    }>;
}
