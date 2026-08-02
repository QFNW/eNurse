import { Controller, Get, Put, Delete, Body } from '@nestjs/common';
import { IsOptional, IsString } from 'class-validator';
import { UsersService } from './users.service';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../common/decorators/current-user.decorator';

class UpdateProfileDto {
  @IsOptional()
  @IsString()
  nickname?: string;
}

@Controller('api/v1/users')
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Get('me')
  async getProfile(@CurrentUser() user: JwtPayload) {
    return this.usersService.findById(user.sub);
  }

  @Put('me')
  async updateProfile(
    @CurrentUser() user: JwtPayload,
    @Body() body: UpdateProfileDto,
  ) {
    return this.usersService.update(user.sub, body);
  }

  @Delete('me')
  async deleteAccount(@CurrentUser() user: JwtPayload) {
    return this.usersService.delete(user.sub);
  }
}
