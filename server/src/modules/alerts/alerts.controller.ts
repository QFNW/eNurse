import { Controller, Get, Put, Param, Query } from '@nestjs/common';
import { AlertsService } from './alerts.service';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../common/decorators/current-user.decorator';

@Controller('api/v1/alerts')
export class AlertsController {
  constructor(private readonly alertsService: AlertsService) {}

  @Get()
  findByUser(
    @CurrentUser() user: JwtPayload,
    @Query('filter') filter?: string,
  ) {
    return this.alertsService.findByUser(user.sub, filter);
  }

  @Put(':id/resolve')
  resolve(
    @CurrentUser() user: JwtPayload,
    @Param('id') alertId: string,
  ) {
    return this.alertsService.resolve(user.sub, alertId);
  }
}
