"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.DateOffsetInterceptor = void 0;
const common_1 = require("@nestjs/common");
const rxjs_1 = require("rxjs");
let DateOffsetInterceptor = class DateOffsetInterceptor {
    intercept(_context, next) {
        return next.handle().pipe((0, rxjs_1.map)((data) => convertDates(data)));
    }
};
exports.DateOffsetInterceptor = DateOffsetInterceptor;
exports.DateOffsetInterceptor = DateOffsetInterceptor = __decorate([
    (0, common_1.Injectable)()
], DateOffsetInterceptor);
function convertDates(value) {
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
function toIsoWithOffset(date) {
    return new Date(date.getTime() + 8 * 3600_000).toISOString().replace('Z', '+08:00');
}
//# sourceMappingURL=date-offset.interceptor.js.map