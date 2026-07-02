import { Module } from '@nestjs/common';
import { BullModule } from '@nestjs/bull';
import { WechatBotService } from './wechat-bot.service';
import { AlertMessageProcessor } from './processors/alert-message.processor';

@Module({
  imports: [
    BullModule.registerQueue({ name: 'alerts' }),
  ],
  providers: [WechatBotService, AlertMessageProcessor],
  exports: [WechatBotService],
})
export class WechatBotModule {}
