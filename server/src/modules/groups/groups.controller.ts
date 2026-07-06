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
import { GroupsService } from './groups.service';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../common/decorators/current-user.decorator';
import { Roles } from '../../common/decorators/roles.decorator';
import { Role } from '../../../generated/prisma/client';
import {
  CreateGroupDto,
  UpdateGroupDto,
  JoinGroupDto,
  UpdateMemberStrategyDto,
  LockStrategyDto,
} from './dto/group.dto';

@Controller('api/v1/groups')
export class GroupsController {
  constructor(private readonly groupsService: GroupsService) {}

  // ========== Group CRUD ==========

  @Post()
  @Roles(Role.NURSE)
  create(
    @CurrentUser() user: JwtPayload,
    @Body() dto: CreateGroupDto,
  ) {
    return this.groupsService.create(user.sub, dto);
  }

  @Get()
  getMyGroups(@CurrentUser() user: JwtPayload) {
    return this.groupsService.findByUser(user.sub, user.role);
  }

  @Get(':id')
  getById(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
  ) {
    return this.groupsService.findById(user.sub, id);
  }

  @Put(':id')
  @Roles(Role.NURSE)
  update(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Body() dto: UpdateGroupDto,
  ) {
    return this.groupsService.update(user.sub, id, dto);
  }

  @Delete(':id')
  @Roles(Role.NURSE)
  remove(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
  ) {
    return this.groupsService.remove(user.sub, id);
  }

  @Post(':id/regenerate-invite')
  @Roles(Role.NURSE)
  regenerateInvite(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
  ) {
    return this.groupsService.regenerateInvite(user.sub, id);
  }

  // ========== Join ==========

  @Post('join')
  @Roles(Role.PATIENT)
  joinByInvite(
    @CurrentUser() user: JwtPayload,
    @Body() dto: JoinGroupDto,
  ) {
    return this.groupsService.joinByInvite(user.sub, dto);
  }

  // ========== Members ==========

  @Get(':id/members')
  @Roles(Role.NURSE)
  getMembers(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
  ) {
    return this.groupsService.getMembers(user.sub, id);
  }

  @Post(':id/leave')
  @Roles(Role.PATIENT)
  leave(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
  ) {
    return this.groupsService.leave(user.sub, id);
  }

  @Delete(':id/members/:userId')
  @Roles(Role.NURSE)
  removeMember(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Param('userId') memberId: string,
  ) {
    return this.groupsService.removeMember(user.sub, id, memberId);
  }

  // ========== Member detail views ==========

  @Get(':id/members/:userId/alerts')
  @Roles(Role.NURSE)
  getMemberAlerts(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Param('userId') memberId: string,
  ) {
    return this.groupsService.getMemberAlerts(user.sub, id, memberId);
  }

  @Get(':id/members/:userId/events')
  @Roles(Role.NURSE)
  getMemberEvents(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Param('userId') memberId: string,
    @Query('from') from?: string,
    @Query('to') to?: string,
  ) {
    return this.groupsService.getMemberEvents(user.sub, id, memberId, from, to);
  }

  @Get(':id/members/:userId/strategies')
  @Roles(Role.NURSE)
  getMemberStrategies(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Param('userId') memberId: string,
  ) {
    return this.groupsService.getMemberStrategies(user.sub, id, memberId);
  }

  @Put(':id/members/:userId/strategies')
  @Roles(Role.NURSE)
  updateMemberStrategies(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Param('userId') memberId: string,
    @Body() dto: UpdateMemberStrategyDto,
  ) {
    return this.groupsService.updateMemberStrategies(user.sub, id, memberId, dto);
  }

  @Put(':id/members/:userId/strategies/lock')
  @Roles(Role.NURSE)
  toggleLock(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Param('userId') memberId: string,
    @Body() dto: LockStrategyDto,
  ) {
    return this.groupsService.toggleLock(
      user.sub,
      id,
      memberId,
      dto.target,
      dto.scheduleId,
    );
  }

  @Get(':id/members/:userId/calendar')
  @Roles(Role.NURSE)
  getMemberCalendar(
    @CurrentUser() user: JwtPayload,
    @Param('id') id: string,
    @Param('userId') memberId: string,
    @Query('year') year: string,
    @Query('month') month: string,
  ) {
    return this.groupsService.getMemberCalendar(
      user.sub,
      id,
      memberId,
      parseInt(year, 10) || new Date().getFullYear(),
      parseInt(month, 10) || (new Date().getMonth() + 1),
    );
  }
}
