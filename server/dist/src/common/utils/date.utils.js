"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.parseDate = parseDate;
const common_1 = require("@nestjs/common");
function parseDate(value, fieldName = 'date') {
    if (!value) {
        throw new common_1.BadRequestException(`${fieldName} is required`);
    }
    const d = new Date(value);
    if (isNaN(d.getTime())) {
        throw new common_1.BadRequestException(`${fieldName} '${value}' is not a valid date`);
    }
    return d;
}
//# sourceMappingURL=date.utils.js.map