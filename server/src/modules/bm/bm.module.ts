import { Module } from '@nestjs/common';
import { BmService } from './bm.service';
import { BmController } from './bm.controller';

@Module({
  controllers: [BmController],
  providers: [BmService],
  exports: [BmService],
})
export class BmModule {}
