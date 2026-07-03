import { OnModuleInit } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
export declare class WechatBotService implements OnModuleInit {
    private configService;
    private readonly logger;
    private bot;
    private isReady;
    constructor(configService: ConfigService);
    onModuleInit(): Promise<void>;
    sendPrivateMessage(wxid: string, message: string): Promise<boolean>;
    sendGroupMessage(groupWxid: string, message: string): Promise<boolean>;
    isBotReady(): boolean;
    private handleIncomingMessage;
}
