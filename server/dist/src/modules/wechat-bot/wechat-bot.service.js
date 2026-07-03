"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var WechatBotService_1;
Object.defineProperty(exports, "__esModule", { value: true });
exports.WechatBotService = void 0;
const common_1 = require("@nestjs/common");
const config_1 = require("@nestjs/config");
let WechatBotService = WechatBotService_1 = class WechatBotService {
    configService;
    logger = new common_1.Logger(WechatBotService_1.name);
    bot = null;
    isReady = false;
    constructor(configService) {
        this.configService = configService;
    }
    async onModuleInit() {
        const token = this.configService.get('PADLOCAL_TOKEN') ||
            process.env.PADLOCAL_TOKEN;
        if (!token) {
            this.logger.warn('PADLOCAL_TOKEN not set — WeChat Bot disabled. ' +
                'Set it in .env to enable WeChat notifications.');
            return;
        }
        try {
            const { WechatyBuilder } = await import('wechaty');
            this.bot = WechatyBuilder.build({
                name: 'eNurseBot',
                puppet: `wechaty-puppet-padlocal`,
                puppetOptions: { token },
            });
            this.bot.on('scan', (qrcode, status) => {
                const qrUrl = `https://wechaty.js.org/qrcode/${encodeURIComponent(qrcode)}`;
                this.logger.log(`Scan QR Code to login (status: ${status}): ${qrUrl}`);
            });
            this.bot.on('login', (user) => {
                this.logger.log(`WeChat Bot logged in as: ${user.name()}`);
                this.isReady = true;
            });
            this.bot.on('logout', (user) => {
                this.logger.warn(`WeChat Bot logged out: ${user.name()}`);
                this.isReady = false;
            });
            this.bot.on('error', (err) => {
                this.logger.error(`WeChat Bot error: ${err.message}`);
            });
            this.bot.on('message', async (msg) => {
                await this.handleIncomingMessage(msg);
            });
            await this.bot.start();
            this.logger.log('WeChat Bot starting... waiting for QR scan');
        }
        catch (err) {
            this.logger.error(`Failed to start WeChat Bot: ${err.message}. ` +
                'Make sure wechaty-puppet-padlocal is installed: ' +
                'npm install wechaty-puppet-padlocal');
        }
    }
    async sendPrivateMessage(wxid, message) {
        if (!this.isReady || !this.bot) {
            this.logger.warn(`Bot not ready, skipping private message to ${wxid}`);
            return false;
        }
        try {
            const contact = await this.bot.Contact.find({ id: wxid });
            if (contact) {
                await contact.say(message);
                this.logger.log(`Private message sent to ${wxid}`);
                return true;
            }
            this.logger.warn(`Contact not found: ${wxid}`);
            return false;
        }
        catch (err) {
            this.logger.error(`Failed to send private message: ${err.message}`);
            return false;
        }
    }
    async sendGroupMessage(groupWxid, message) {
        if (!this.isReady || !this.bot) {
            this.logger.warn(`Bot not ready, skipping group message to ${groupWxid}`);
            return false;
        }
        try {
            const room = await this.bot.Room.find({ id: groupWxid });
            if (room) {
                await room.say(`@所有人\n${message}`);
                this.logger.log(`Group message sent to ${groupWxid}`);
                return true;
            }
            this.logger.warn(`Room not found: ${groupWxid}`);
            return false;
        }
        catch (err) {
            this.logger.error(`Failed to send group message: ${err.message}`);
            return false;
        }
    }
    isBotReady() {
        return this.isReady;
    }
    async handleIncomingMessage(msg) {
        try {
            const text = msg.text();
            if (!text)
                return;
            if (text.startsWith('/bind')) {
                await msg.say('请在eNurse App中完成微信绑定，输入你的邮箱即可。');
            }
        }
        catch {
        }
    }
};
exports.WechatBotService = WechatBotService;
exports.WechatBotService = WechatBotService = WechatBotService_1 = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [config_1.ConfigService])
], WechatBotService);
//# sourceMappingURL=wechat-bot.service.js.map