import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  Query,
} from '@nestjs/common';
import { BmService } from './bm.service';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../common/decorators/current-user.decorator';
import { Roles } from '../../common/decorators/roles.decorator';
import { Role } from '../../../generated/prisma/client';
import {
  CreateBmRecordDto,
  UpdateBmRecordDto,
  UpdateBmSettingsDto,
} from './dto/bm.dto';

@Controller('api/v1/bm')
export class BmController {
  constructor(private readonly bmService: BmService) {}

  // ========== Records ==========

  @Post('records')
  @Roles(Role.PATIENT)
  createRecord(
    @CurrentUser() user: JwtPayload,
    @Body() dto: CreateBmRecordDto,
  ) {
    return this.bmService.createRecord(user.sub, dto);
  }

  @Get('records')
  @Roles(Role.PATIENT)
  getRecords(
    @CurrentUser() user: JwtPayload,
    @Query('from') from?: string,
    @Query('to') to?: string,
    @Query('page') page?: string,
    @Query('pageSize') pageSize?: string,
  ) {
    return this.bmService.getRecords(user.sub, { from, to, page, pageSize });
  }

  @Get('records/:id')
  @Roles(Role.PATIENT)
  getRecordById(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
  ) {
    return this.bmService.getRecordById(user.sub, id);
  }

  @Put('records/:id')
  @Roles(Role.PATIENT)
  updateRecord(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Body() dto: UpdateBmRecordDto,
  ) {
    return this.bmService.updateRecord(user.sub, id, dto);
  }

  @Delete('records/:id')
  @Roles(Role.PATIENT)
  deleteRecord(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
  ) {
    return this.bmService.deleteRecord(user.sub, id);
  }

  // ========== Calendar ==========

  @Get('calendar')
  @Roles(Role.PATIENT)
  getCalendar(
    @CurrentUser() user: JwtPayload,
    @Query('year') year: string,
    @Query('month') month: string,
  ) {
    return this.bmService.getCalendar(
      user.sub,
      parseInt(year, 10) || new Date().getFullYear(),
      parseInt(month, 10) || (new Date().getMonth() + 1),
    );
  }

  // ========== Settings ==========

  @Get('settings')
  @Roles(Role.PATIENT)
  getSettings(@CurrentUser() user: JwtPayload) {
    return this.bmService.getSettings(user.sub);
  }

  @Put('settings')
  @Roles(Role.PATIENT)
  updateSettings(
    @CurrentUser() user: JwtPayload,
    @Body() dto: UpdateBmSettingsDto,
  ) {
    return this.bmService.updateSettings(user.sub, dto);
  }
}
