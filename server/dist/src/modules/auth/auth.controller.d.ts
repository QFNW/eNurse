import { AuthService } from './auth.service';
import { RegisterDto } from './dto/register.dto';
import { LoginDto, RefreshDto } from './dto/login.dto';
export declare class AuthController {
    private readonly authService;
    constructor(authService: AuthService);
    register(dto: RegisterDto): Promise<{
        user: {
            id: string;
            username: string;
            email: string;
            role: import("../../../generated/prisma/enums").Role;
            displayName: string;
        };
        accessToken: string;
        refreshToken: string;
    }>;
    login(dto: LoginDto): Promise<{
        user: {
            id: string;
            username: string;
            email: string;
            role: import("../../../generated/prisma/enums").Role;
            displayName: string;
        };
        accessToken: string;
        refreshToken: string;
    }>;
    refresh(dto: RefreshDto): Promise<{
        user: {
            id: string;
            username: string;
            email: string;
            role: import("../../../generated/prisma/enums").Role;
            displayName: string;
        };
        accessToken: string;
        refreshToken: string;
    }>;
    logout(dto: RefreshDto): Promise<{
        message: string;
    }>;
}
