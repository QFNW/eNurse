import { Module } from '@nestjs/common';
import { BullModule } from '@nestjs/bull';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { WechatBotService } from './wechat-bot.service';
import { AlertMessageProcessor } from './processors/alert-message.processor';

@Module({
  imports: [
    BullModule.registerQueueAsync({
      name: 'alerts',
      imports: [ConfigModule],
      useFactory: (configService: ConfigService) => ({
        redis: {
          host: configService.get<string>('redis.host') || 'localhost',
          port: configService.get<number>('redis.port') || 6379,
        },
      }),
      inject: [ConfigService],
    }),
  ],
  providers: [WechatBotService, AlertMessageProcessor],
  exports: [WechatBotService],
})
export class WechatBotModule {}
