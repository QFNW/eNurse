"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.BmController = void 0;
const common_1 = require("@nestjs/common");
const bm_service_1 = require("./bm.service");
const current_user_decorator_1 = require("../../common/decorators/current-user.decorator");
const roles_decorator_1 = require("../../common/decorators/roles.decorator");
const client_1 = require("../../../generated/prisma/client");
const bm_dto_1 = require("./dto/bm.dto");
let BmController = class BmController {
    bmService;
    constructor(bmService) {
        this.bmService = bmService;
    }
    createRecord(user, dto) {
        return this.bmService.createRecord(user.sub, dto);
    }
    getRecords(user, from, to, page, pageSize) {
        return this.bmService.getRecords(user.sub, { from, to, page, pageSize });
    }
    getRecordById(user, id) {
        return this.bmService.getRecordById(user.sub, id);
    }
    updateRecord(user, id, dto) {
        return this.bmService.updateRecord(user.sub, id, dto);
    }
    deleteRecord(user, id) {
        return this.bmService.deleteRecord(user.sub, id);
    }
    getCalendar(user, year, month) {
        return this.bmService.getCalendar(user.sub, parseInt(year, 10) || new Date().getFullYear(), parseInt(month, 10) || (new Date().getMonth() + 1));
    }
    getSettings(user) {
        return this.bmService.getSettings(user.sub);
    }
    updateSettings(user, dto) {
        return this.bmService.updateSettings(user.sub, dto);
    }
};
exports.BmController = BmController;
__decorate([
    (0, common_1.Post)('records'),
    (0, roles_decorator_1.Roles)(client_1.Role.PATIENT),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, bm_dto_1.CreateBmRecordDto]),
    __metadata("design:returntype", void 0)
], BmController.prototype, "createRecord", null);
__decorate([
    (0, common_1.Get)('records'),
    (0, roles_decorator_1.Roles)(client_1.Role.PATIENT),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Query)('from')),
    __param(2, (0, common_1.Query)('to')),
    __param(3, (0, common_1.Query)('page')),
    __param(4, (0, common_1.Query)('pageSize')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String, String, String, String]),
    __metadata("design:returntype", void 0)
], BmController.prototype, "getRecords", null);
__decorate([
    (0, common_1.Get)('records/:id'),
    (0, roles_decorator_1.Roles)(client_1.Role.PATIENT),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String]),
    __metadata("design:returntype", void 0)
], BmController.prototype, "getRecordById", null);
__decorate([
    (0, common_1.Put)('records/:id'),
    (0, roles_decorator_1.Roles)(client_1.Role.PATIENT),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Param)('id')),
    __param(2, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String, bm_dto_1.UpdateBmRecordDto]),
    __metadata("design:returntype", void 0)
], BmController.prototype, "updateRecord", null);
__decorate([
    (0, common_1.Delete)('records/:id'),
    (0, roles_decorator_1.Roles)(client_1.Role.PATIENT),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String]),
    __metadata("design:returntype", void 0)
], BmController.prototype, "deleteRecord", null);
__decorate([
    (0, common_1.Get)('calendar'),
    (0, roles_decorator_1.Roles)(client_1.Role.PATIENT),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Query)('year')),
    __param(2, (0, common_1.Query)('month')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String, String]),
    __metadata("design:returntype", void 0)
], BmController.prototype, "getCalendar", null);
__decorate([
    (0, common_1.Get)('settings'),
    (0, roles_decorator_1.Roles)(client_1.Role.PATIENT),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", void 0)
], BmController.prototype, "getSettings", null);
__decorate([
    (0, common_1.Put)('settings'),
    (0, roles_decorator_1.Roles)(client_1.Role.PATIENT),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, bm_dto_1.UpdateBmSettingsDto]),
    __metadata("design:returntype", void 0)
], BmController.prototype, "updateSettings", null);
exports.BmController = BmController = __decorate([
    (0, common_1.Controller)('api/v1/bm'),
    __metadata("design:paramtypes", [bm_service_1.BmService])
], BmController);
//# sourceMappingURL=bm.controller.js.map