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
exports.GroupsController = void 0;
const common_1 = require("@nestjs/common");
const groups_service_1 = require("./groups.service");
const current_user_decorator_1 = require("../../common/decorators/current-user.decorator");
const roles_decorator_1 = require("../../common/decorators/roles.decorator");
const client_1 = require("../../../generated/prisma/client");
const group_dto_1 = require("./dto/group.dto");
let GroupsController = class GroupsController {
    groupsService;
    constructor(groupsService) {
        this.groupsService = groupsService;
    }
    create(user, dto) {
        return this.groupsService.create(user.sub, dto);
    }
    getMyGroups(user) {
        return this.groupsService.findByUser(user.sub, user.role);
    }
    getById(user, id) {
        return this.groupsService.findById(user.sub, id);
    }
    update(user, id, dto) {
        return this.groupsService.update(user.sub, id, dto);
    }
    remove(user, id) {
        return this.groupsService.remove(user.sub, id);
    }
    regenerateInvite(user, id) {
        return this.groupsService.regenerateInvite(user.sub, id);
    }
    joinByInvite(user, dto) {
        return this.groupsService.joinByInvite(user.sub, dto);
    }
    getMembers(user, id) {
        return this.groupsService.getMembers(user.sub, id);
    }
    removeMember(user, id, memberId) {
        return this.groupsService.removeMember(user.sub, id, memberId);
    }
    getMemberAlerts(user, id, memberId) {
        return this.groupsService.getMemberAlerts(user.sub, id, memberId);
    }
    getMemberEvents(user, id, memberId, from, to) {
        return this.groupsService.getMemberEvents(user.sub, id, memberId, from, to);
    }
    getMemberStrategies(user, id, memberId) {
        return this.groupsService.getMemberStrategies(user.sub, id, memberId);
    }
    updateMemberStrategies(user, id, memberId, dto) {
        return this.groupsService.updateMemberStrategies(user.sub, id, memberId, dto);
    }
    toggleLock(user, id, memberId, dto) {
        return this.groupsService.toggleLock(user.sub, id, memberId, dto.target, dto.scheduleId);
    }
    getMemberCalendar(user, id, memberId, year, month) {
        return this.groupsService.getMemberCalendar(user.sub, id, memberId, parseInt(year, 10), parseInt(month, 10));
    }
};
exports.GroupsController = GroupsController;
__decorate([
    (0, common_1.Post)(),
    (0, roles_decorator_1.Roles)(client_1.Role.NURSE),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, group_dto_1.CreateGroupDto]),
    __metadata("design:returntype", void 0)
], GroupsController.prototype, "create", null);
__decorate([
    (0, common_1.Get)(),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", void 0)
], GroupsController.prototype, "getMyGroups", null);
__decorate([
    (0, common_1.Get)(':id'),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String]),
    __metadata("design:returntype", void 0)
], GroupsController.prototype, "getById", null);
__decorate([
    (0, common_1.Put)(':id'),
    (0, roles_decorator_1.Roles)(client_1.Role.NURSE),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Param)('id')),
    __param(2, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String, group_dto_1.UpdateGroupDto]),
    __metadata("design:returntype", void 0)
], GroupsController.prototype, "update", null);
__decorate([
    (0, common_1.Delete)(':id'),
    (0, roles_decorator_1.Roles)(client_1.Role.NURSE),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String]),
    __metadata("design:returntype", void 0)
], GroupsController.prototype, "remove", null);
__decorate([
    (0, common_1.Post)(':id/regenerate-invite'),
    (0, roles_decorator_1.Roles)(client_1.Role.NURSE),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String]),
    __metadata("design:returntype", void 0)
], GroupsController.prototype, "regenerateInvite", null);
__decorate([
    (0, common_1.Post)('join'),
    (0, roles_decorator_1.Roles)(client_1.Role.PATIENT),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, group_dto_1.JoinGroupDto]),
    __metadata("design:returntype", void 0)
], GroupsController.prototype, "joinByInvite", null);
__decorate([
    (0, common_1.Get)(':id/members'),
    (0, roles_decorator_1.Roles)(client_1.Role.NURSE),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String]),
    __metadata("design:returntype", void 0)
], GroupsController.prototype, "getMembers", null);
__decorate([
    (0, common_1.Delete)(':id/members/:userId'),
    (0, roles_decorator_1.Roles)(client_1.Role.NURSE),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Param)('id')),
    __param(2, (0, common_1.Param)('userId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String, String]),
    __metadata("design:returntype", void 0)
], GroupsController.prototype, "removeMember", null);
__decorate([
    (0, common_1.Get)(':id/members/:userId/alerts'),
    (0, roles_decorator_1.Roles)(client_1.Role.NURSE),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Param)('id')),
    __param(2, (0, common_1.Param)('userId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String, String]),
    __metadata("design:returntype", void 0)
], GroupsController.prototype, "getMemberAlerts", null);
__decorate([
    (0, common_1.Get)(':id/members/:userId/events'),
    (0, roles_decorator_1.Roles)(client_1.Role.NURSE),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Param)('id')),
    __param(2, (0, common_1.Param)('userId')),
    __param(3, (0, common_1.Query)('from')),
    __param(4, (0, common_1.Query)('to')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String, String, String, String]),
    __metadata("design:returntype", void 0)
], GroupsController.prototype, "getMemberEvents", null);
__decorate([
    (0, common_1.Get)(':id/members/:userId/strategies'),
    (0, roles_decorator_1.Roles)(client_1.Role.NURSE),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Param)('id')),
    __param(2, (0, common_1.Param)('userId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String, String]),
    __metadata("design:returntype", void 0)
], GroupsController.prototype, "getMemberStrategies", null);
__decorate([
    (0, common_1.Put)(':id/members/:userId/strategies'),
    (0, roles_decorator_1.Roles)(client_1.Role.NURSE),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Param)('id')),
    __param(2, (0, common_1.Param)('userId')),
    __param(3, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String, String, group_dto_1.UpdateMemberStrategyDto]),
    __metadata("design:returntype", void 0)
], GroupsController.prototype, "updateMemberStrategies", null);
__decorate([
    (0, common_1.Put)(':id/members/:userId/strategies/lock'),
    (0, roles_decorator_1.Roles)(client_1.Role.NURSE),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Param)('id')),
    __param(2, (0, common_1.Param)('userId')),
    __param(3, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String, String, group_dto_1.LockStrategyDto]),
    __metadata("design:returntype", void 0)
], GroupsController.prototype, "toggleLock", null);
__decorate([
    (0, common_1.Get)(':id/members/:userId/calendar'),
    (0, roles_decorator_1.Roles)(client_1.Role.NURSE),
    __param(0, (0, current_user_decorator_1.CurrentUser)()),
    __param(1, (0, common_1.Param)('id')),
    __param(2, (0, common_1.Param)('userId')),
    __param(3, (0, common_1.Query)('year')),
    __param(4, (0, common_1.Query)('month')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, String, String, String, String]),
    __metadata("design:returntype", void 0)
], GroupsController.prototype, "getMemberCalendar", null);
exports.GroupsController = GroupsController = __decorate([
    (0, common_1.Controller)('api/v1/groups'),
    __metadata("design:paramtypes", [groups_service_1.GroupsService])
], GroupsController);
//# sourceMappingURL=groups.controller.js.map