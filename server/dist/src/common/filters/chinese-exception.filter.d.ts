import { ExceptionFilter, ArgumentsHost } from '@nestjs/common';
export declare class ChineseExceptionFilter implements ExceptionFilter {
    private readonly logger;
    private static readonly TRANSLATIONS;
    catch(exception: unknown, host: ArgumentsHost): void;
}
