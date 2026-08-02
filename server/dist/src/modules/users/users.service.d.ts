import { PrismaService } from '../../prisma/prisma.service';
export declare class UsersService {
    private prisma;
    constructor(prisma: PrismaService);
    findById(id: string): Promise<{
        id: string;
        email: string;
        displayName: string;
        nickname: string | null;
        role: import("../../../generated/prisma/enums").Role;
        createdAt: Date;
    } | null>;
    update(id: string, data: {
        nickname?: string;
        displayName?: string;
    }): Promise<{
        id: string;
        email: string;
        displayName: string;
        nickname: string | null;
        role: import("../../../generated/prisma/enums").Role;
    }>;
    changePassword(id: string, oldPassword: string, newPassword: string): Promise<{
        message: string;
    }>;
    delete(id: string): Promise<{
        message: string;
    }>;
}
