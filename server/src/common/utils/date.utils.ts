import { BadRequestException } from '@nestjs/common';

/** Parse a date string with validation. Throws 400 on invalid input. */
export function parseDate(value: string | undefined | null, fieldName = 'date'): Date {
  if (!value) {
    throw new BadRequestException(`${fieldName} is required`);
  }
  const d = new Date(value);
  if (isNaN(d.getTime())) {
    throw new BadRequestException(`${fieldName} '${value}' is not a valid date`);
  }
  return d;
}
