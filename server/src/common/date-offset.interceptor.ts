import {
  CallHandler,
  ExecutionContext,
  Injectable,
  NestInterceptor,
} from '@nestjs/common';
import { Observable, map } from 'rxjs';

/**
 * 全局时间序列化：把响应中的 Date 输出为 UTC+8 偏移的 ISO 字符串
 * （如 2026-08-10T20:30:00.000+08:00），客户端直接截取/解析即为北京时间。
 * 存储层仍为 UTC（Prisma DateTime 不变），仅影响 JSON 输出。
 */
@Injectable()
export class DateOffsetInterceptor implements NestInterceptor {
  intercept(_context: ExecutionContext, next: CallHandler): Observable<any> {
    return next.handle().pipe(map((data) => convertDates(data)));
  }
}

function convertDates(value: any): any {
  if (value instanceof Date) {
    return toIsoWithOffset(value);
  }
  if (Array.isArray(value)) {
    return value.map(convertDates);
  }
  if (value && typeof value === 'object') {
    for (const key of Object.keys(value)) {
      value[key] = convertDates(value[key]);
    }
    return value;
  }
  return value;
}

function toIsoWithOffset(date: Date): string {
  // UTC+8（Asia/Shanghai，无夏令时）墙钟输出，带 +08:00 偏移
  return new Date(date.getTime() + 8 * 3600_000).toISOString().replace('Z', '+08:00');
}
