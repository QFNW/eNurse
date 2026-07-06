import { IsString } from 'class-validator';

export class LoginDto {
  @IsString()
  login: string; // 用户名或邮箱（含@按邮箱查找，否则按用户名）

  @IsString()
  password: string;
}

export class RefreshDto {
  @IsString()
  refreshToken: string;
}
