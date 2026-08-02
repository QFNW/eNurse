import { PrismaService } from '../../prisma/prisma.service';
import { EmailService } from '../auth/email.service';
import { ConfigService } from '@nestjs/config';
export declare class FeedbackService {
    private prisma;
    private emailService;
    private configService;
    constructor(prisma: PrismaService, emailService: EmailService, configService: ConfigService);
    create(userId: string, content: string): Promise<{
        message: string;
    }>;
}
