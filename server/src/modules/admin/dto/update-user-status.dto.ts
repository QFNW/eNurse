import { IsBoolean } from 'class-validator';

export class UpdateUserStatusDto {
  @IsBoolean({ message: 'isDisabled 必须是布尔值' })
  isDisabled!: boolean;
}
