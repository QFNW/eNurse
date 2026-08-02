import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  Query,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { AdminService } from './admin.service';
import { Roles } from '../../common/decorators/roles.decorator';
import { Public } from '../../common/decorators/public.decorator';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { Role } from '../../../generated/prisma/client';
import { UpdateUserStatusDto } from './dto/update-user-status.dto';
import { ResetPasswordDto } from './dto/reset-password.dto';

@Controller('api/v1/admin')
export class AdminController {
  constructor(private readonly adminService: AdminService) {}

  @Public()
  @Post('login')
  @HttpCode(HttpStatus.OK)
  async login(@Body() body: { password: string }) {
    return this.adminService.login(body.password);
  }

  @Get('users')
  @Roles(Role.ADMIN)
  getUsers(
    @Query('search') search?: string,
    @Query('page') page?: string,
    @Query('pageSize') pageSize?: string,
  ) {
    return this.adminService.getUsers(
      search,
      parseInt(page || '1', 10) || 1,
      parseInt(pageSize || '20', 10) || 20,
    );
  }

  @Put('users/:id/role')
  @Roles(Role.ADMIN)
  updateUserRole(
    @Param('id') userId: string,
    @Body() body: { role: string },
  ) {
    return this.adminService.updateUserRole(userId, body.role);
  }

  @Get('users/:id')
  @Roles(Role.ADMIN)
  getUserDetail(@Param('id') userId: string) {
    return this.adminService.getUserDetail(userId);
  }

  @Put('users/:id/status')
  @Roles(Role.ADMIN)
  setUserStatus(
    @Param('id') userId: string,
    @Body() body: UpdateUserStatusDto,
    @CurrentUser('sub') requesterId?: string,
  ) {
    return this.adminService.setUserStatus(userId, body.isDisabled, requesterId);
  }

  @Put('users/:id/password')
  @Roles(Role.ADMIN)
  resetPassword(
    @Param('id') userId: string,
    @Body() body: ResetPasswordDto,
  ) {
    return this.adminService.resetPassword(userId, body.newPassword);
  }

  @Get('groups')
  @Roles(Role.ADMIN)
  getGroups(
    @Query('page') page?: string,
    @Query('pageSize') pageSize?: string,
  ) {
    return this.adminService.getGroups(
      parseInt(page || '1', 10) || 1,
      parseInt(pageSize || '20', 10) || 20,
    );
  }

  @Get('stats')
  @Roles(Role.ADMIN)
  getStats() {
    return this.adminService.getStats();
  }
}
