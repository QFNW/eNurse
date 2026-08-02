import { Body, Controller, HttpCode, HttpStatus, Post } from '@nestjs/common';
import { FeedbackService } from './feedback.service';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { CreateFeedbackDto } from './dto/create-feedback.dto';

@Controller('api/v1/feedback')
export class FeedbackController {
  constructor(private readonly feedbackService: FeedbackService) {}

  @Post()
  @HttpCode(HttpStatus.CREATED)
  create(@CurrentUser('sub') userId: string, @Body() dto: CreateFeedbackDto) {
    return this.feedbackService.create(userId, dto.content);
  }
}
