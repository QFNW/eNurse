import { IsString, MinLength, MaxLength } from 'class-validator';

export class ResetPasswordDto {
  @IsString({ message: '新密码格式错误' })
  @MinLength(6, { message: '密码至少 6 位' })
  @MaxLength(72, { message: '密码最长 72 位' })
  newPassword!: string;
}
