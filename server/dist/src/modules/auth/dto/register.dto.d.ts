import { Role } from '../../../../generated/prisma/client';
export declare class RegisterDto {
    username: string;
    displayName: string;
    email: string;
    password: string;
    role: Role;
}
