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
import { MedicationsService } from './medications.service';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../common/decorators/current-user.decorator';
import { Roles } from '../../common/decorators/roles.decorator';
import { Role } from '../../../generated/prisma/client';
import {
  CreateMedicationDto,
  UpdateMedicationDto,
  CreateScheduleDto,
  UpdateScheduleDto,
  CreateMedicationRecordDto,
} from './dto/medication.dto';

@Controller('api/v1/medications')
export class MedicationsController {
  constructor(private readonly medicationsService: MedicationsService) {}

  // ========== Medications ==========

  @Post()
  @Roles(Role.PATIENT)
  create(
    @CurrentUser() user: JwtPayload,
    @Body() dto: CreateMedicationDto,
  ) {
    return this.medicationsService.create(user.sub, dto);
  }

  @Get()
  @Roles(Role.PATIENT)
  findAll(@CurrentUser() user: JwtPayload) {
    return this.medicationsService.findAll(user.sub);
  }

  @Get(':id')
  @Roles(Role.PATIENT)
  findById(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
  ) {
    return this.medicationsService.findById(user.sub, id);
  }

  @Put(':id')
  @Roles(Role.PATIENT)
  update(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Body() dto: UpdateMedicationDto,
  ) {
    return this.medicationsService.update(user.sub, id, dto);
  }

  @Delete(':id')
  @Roles(Role.PATIENT)
  remove(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
  ) {
    return this.medicationsService.remove(user.sub, id);
  }

  // ========== Schedules ==========

  @Post(':id/schedules')
  @Roles(Role.PATIENT)
  createSchedule(
    @CurrentUser() user: JwtPayload,
    @Param('id') medicationId: string,
    @Body() dto: CreateScheduleDto,
  ) {
    return this.medicationsService.createSchedule(user.sub, medicationId, dto);
  }

  @Put('schedules/:id')
  @Roles(Role.PATIENT)
  updateSchedule(
    @CurrentUser() user: JwtPayload,
    @Param('id') scheduleId: string,
    @Body() dto: UpdateScheduleDto,
  ) {
    return this.medicationsService.updateSchedule(user.sub, scheduleId, dto);
  }

  @Delete('schedules/:id')
  @Roles(Role.PATIENT)
  deleteSchedule(
    @CurrentUser() user: JwtPayload,
    @Param('id') scheduleId: string,
  ) {
    return this.medicationsService.deleteSchedule(user.sub, scheduleId);
  }

  // ========== Records ==========

  @Post('records')
  @Roles(Role.PATIENT)
  createRecord(
    @CurrentUser() user: JwtPayload,
    @Body() dto: CreateMedicationRecordDto,
  ) {
    return this.medicationsService.createRecord(user.sub, dto);
  }

  @Get('records')
  @Roles(Role.PATIENT)
  getRecords(
    @CurrentUser() user: JwtPayload,
    @Query('from') from?: string,
    @Query('to') to?: string,
  ) {
    return this.medicationsService.getRecords(user.sub, from, to);
  }
}
