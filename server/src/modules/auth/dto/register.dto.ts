import { IsEmail, IsString, MinLength, MaxLength, IsEnum, Matches } from 'class-validator';
import { Role } from '../../../../generated/prisma/client';

export class RegisterDto {
  @IsString()
  @Matches(/^[a-zA-Z0-9_]{3,20}$/, { message: '用户名须为3-20位字母数字下划线' })
  username: string;

  @IsString()
  @MaxLength(20, { message: '姓名不超过20字节' })
  displayName: string;

  @IsEmail()
  email: string;

  @IsString()
  @MinLength(8)
  password: string;

  @IsEnum(Role)
  role: Role;
}
