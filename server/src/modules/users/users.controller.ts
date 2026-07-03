import { Controller, Get, Put, Body } from '@nestjs/common';
import { UsersService } from './users.service';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtPayload } from '../../common/decorators/current-user.decorator';

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
    @Body() body: { nickname?: string },
  ) {
    return this.usersService.update(user.sub, body);
  }

}
