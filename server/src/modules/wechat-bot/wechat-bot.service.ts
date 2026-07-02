import { Injectable, Logger, OnModuleInit } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

/**
 * WeChat Bot service using Wechaty + PadLocal (iPad protocol).
 *
 * Requires PADLOCAL_TOKEN in .env.
 * On first run, a QR code URL will be printed to the console — scan with WeChat to login.
 * The bot runs as a secondary account (not personal main account).
 */
@Injectable()
export class WechatBotService implements OnModuleInit {
  private readonly logger = new Logger(WechatBotService.name);
  private bot: any = null;
  private isReady = false;

  constructor(private configService: ConfigService) {}

  async onModuleInit() {
    const token = this.configService.get<string>('PADLOCAL_TOKEN') ||
      process.env.PADLOCAL_TOKEN;

    if (!token) {
      this.logger.warn(
        'PADLOCAL_TOKEN not set — WeChat Bot disabled. ' +
        'Set it in .env to enable WeChat notifications.',
      );
      return;
    }

    try {
      const { WechatyBuilder } = await import('wechaty');

      this.bot = WechatyBuilder.build({
        name: 'eNurseBot',
        puppet: `wechaty-puppet-padlocal`,
        puppetOptions: { token },
      });

      this.bot.on('scan', (qrcode: string, status: number) => {
        const qrUrl = `https://wechaty.js.org/qrcode/${encodeURIComponent(qrcode)}`;
        this.logger.log(
          `Scan QR Code to login (status: ${status}): ${qrUrl}`,
        );
      });

      this.bot.on('login', (user: any) => {
        this.logger.log(`WeChat Bot logged in as: ${user.name()}`);
        this.isReady = true;
      });

      this.bot.on('logout', (user: any) => {
        this.logger.warn(`WeChat Bot logged out: ${user.name()}`);
        this.isReady = false;
      });

      this.bot.on('error', (err: Error) => {
        this.logger.error(`WeChat Bot error: ${err.message}`);
      });

      // Handle incoming messages for binding commands
      this.bot.on('message', async (msg: any) => {
        await this.handleIncomingMessage(msg);
      });

      await this.bot.start();
      this.logger.log('WeChat Bot starting... waiting for QR scan');
    } catch (err: any) {
      this.logger.error(
        `Failed to start WeChat Bot: ${err.message}. ` +
        'Make sure wechaty-puppet-padlocal is installed: ' +
        'npm install wechaty-puppet-padlocal',
      );
    }
  }

  /**
   * Send a private message to a user by their WeChat wxid.
   */
  async sendPrivateMessage(wxid: string, message: string): Promise<boolean> {
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
    } catch (err: any) {
      this.logger.error(`Failed to send private message: ${err.message}`);
      return false;
    }
  }

  /**
   * Send a group message with @all mention.
   * groupWxid: the WeChat room ID (looks like "xxxx@chatroom").
   */
  async sendGroupMessage(
    groupWxid: string,
    message: string,
  ): Promise<boolean> {
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
    } catch (err: any) {
      this.logger.error(`Failed to send group message: ${err.message}`);
      return false;
    }
  }

  isBotReady(): boolean {
    return this.isReady;
  }

  /**
   * Handle incoming messages — e.g. bind WeChat wxid to eNurse account.
   * Users can send "绑定 <code>" to bind their account.
   */
  private async handleIncomingMessage(msg: any) {
    try {
      const text = msg.text();
      if (!text) return;

      // Future: handle bind/unbind commands
      if (text.startsWith('/bind')) {
        await msg.say('请在eNurse App中完成微信绑定，输入你的邮箱即可。');
      }
    } catch {
      // Ignore message handling errors
    }
  }
}
