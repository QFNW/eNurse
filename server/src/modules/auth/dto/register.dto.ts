import { IsEmail, IsString, MinLength, IsEnum } from 'class-validator';
import { Role } from '../../../../generated/prisma/client';

export class RegisterDto {
  @IsEmail()
  email: string;

  @IsString()
  @MinLength(8)
  password: string;

  @IsEnum(Role)
  role: Role;
}
