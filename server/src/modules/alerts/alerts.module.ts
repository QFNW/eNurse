import { Module } from '@nestjs/common';
import { ScheduleModule } from '@nestjs/schedule';
import { AlertsService } from './alerts.service';
import { AlertEngineService } from './alert-engine.service';
import { AlertsController } from './alerts.controller';

@Module({
  imports: [ScheduleModule.forRoot()],
  controllers: [AlertsController],
  providers: [AlertsService, AlertEngineService],
  exports: [AlertsService, AlertEngineService],
})
export class AlertsModule {}
