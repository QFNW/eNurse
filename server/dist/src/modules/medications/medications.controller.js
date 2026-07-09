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
exports.MedicationsController = void 0;
const common_1 = require("@nestjs/common");
const medications_service_1 = require("./medications.service");
const current_user_decorator_1 = require("../../common/decorators/current-user.decorator");
const roles_decorator_1 = require("../../common/decorators/roles.decorator");
const client_1 = require("../../../generated/prisma/client");
const medication_dto_1 = require("./dto/medication.dto");
let MedicationsController = class MedicationsController {
    medicationsService;
    constructor(medicationsService) {
        this.medicationsService = medicationsService;
    }
    create(user, dto) {
        return this.medicationsService.create(user.sub, dto);
    }
    findAll(user) {
        return this.medicationsService.findAll(user.sub);
    }
    createRecord(user, dto) {
        return this.medicationsService.createRecord(user.sub, dto);
    }
    getRecords(user, from, to) {
        return this.medicationsService.getRecords(user.sub, from, to);
    }
    findById(user, id) {
        return this.medicationsService.findById(user.sub, id);
    }
    update(user, id, dto) {
        return this.medicationsService.update(user.sub, id, dto);
    }
    remove(user, id) {
        return this.medicationsService.remove(user.sub, id);
    }
    createSchedule(user, medicationId, dto) {
        return this.medicationsService.createSchedule(user.sub, medicationId, dto);
    }
    updateSchedule(user, scheduleId, dto) {
        return this.medicationsService.updateSchedule(user.sub, scheduleId, dto);
    }
    deleteSchedule(user, scheduleId) {
        return this.medicationsService.deleteSchedule(user.sub, scheduleId);
    }
};
exports.MedicationsController = MedicationsController;
__decorate([
    (0, common_1.Post)(),
    (0, roles_decorator_1.Roles)(client_1.Role.PATIENT),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, medication_dto_1.CreateMedicationDto]),
    __metadata("design:returntype", void 0)
], MedicationsController.prototype, "create", null);
__decorate([
    (0, common_1.Get)(),
    (0, roles_decorator_1.Roles)(client_1.Role.PATIENT),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", void 0)
], MedicationsController.prototype, "findAll", null);
__decorate([
    (0, common_1.Post)('records'),
    (0, roles_decorator_1.Roles)(client_1.Role.PATIENT),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, medication_dto_1.CreateMedicationRecordDto]),
    __metadata("design:returntype", void 0)
], MedicationsController.prototype, "createRecord", null);
__decorate([
    (0, common_1.Get)('records'),
    (0, roles_decorator_1.Roles)(client_1.Role.PATIENT),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Query)('from')),
    __param(2, (0, common_1.Query)('to')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String, String]),
    __metadata("design:returntype", void 0)
], MedicationsController.prototype, "getRecords", null);
__decorate([
    (0, common_1.Get)(':id'),
    (0, roles_decorator_1.Roles)(client_1.Role.PATIENT),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String]),
    __metadata("design:returntype", void 0)
], MedicationsController.prototype, "findById", null);
__decorate([
    (0, common_1.Put)(':id'),
    (0, roles_decorator_1.Roles)(client_1.Role.PATIENT),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Param)('id')),
    __param(2, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String, medication_dto_1.UpdateMedicationDto]),
    __metadata("design:returntype", void 0)
], MedicationsController.prototype, "update", null);
__decorate([
    (0, common_1.Delete)(':id'),
    (0, roles_decorator_1.Roles)(client_1.Role.PATIENT),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String]),
    __metadata("design:returntype", void 0)
], MedicationsController.prototype, "remove", null);
__decorate([
    (0, common_1.Post)(':id/schedules'),
    (0, roles_decorator_1.Roles)(client_1.Role.PATIENT),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Param)('id')),
    __param(2, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String, medication_dto_1.CreateScheduleDto]),
    __metadata("design:returntype", void 0)
], MedicationsController.prototype, "createSchedule", null);
__decorate([
    (0, common_1.Put)('schedules/:id'),
    (0, roles_decorator_1.Roles)(client_1.Role.PATIENT),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Param)('id')),
    __param(2, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String, medication_dto_1.UpdateScheduleDto]),
    __metadata("design:returntype", void 0)
], MedicationsController.prototype, "updateSchedule", null);
__decorate([
    (0, common_1.Delete)('schedules/:id'),
    (0, roles_decorator_1.Roles)(client_1.Role.PATIENT),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String]),
    __metadata("design:returntype", void 0)
], MedicationsController.prototype, "deleteSchedule", null);
exports.MedicationsController = MedicationsController = __decorate([
    (0, common_1.Controller)('api/v1/medications'),
    __metadata("design:paramtypes", [medications_service_1.MedicationsService])
], MedicationsController);
//# sourceMappingURL=medications.controller.js.map