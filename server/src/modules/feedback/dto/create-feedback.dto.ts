import { IsString, IsNotEmpty, MaxLength } from 'class-validator';

export class CreateFeedbackDto {
  @IsString()
  @IsNotEmpty({ message: '反馈内容不能为空' })
  @MaxLength(1000, { message: '反馈内容最多 1000 字' })
  content: string;
}
