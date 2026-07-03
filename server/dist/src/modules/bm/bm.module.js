"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.BmModule = void 0;
const common_1 = require("@nestjs/common");
const bm_service_1 = require("./bm.service");
const bm_controller_1 = require("./bm.controller");
let BmModule = class BmModule {
};
exports.BmModule = BmModule;
exports.BmModule = BmModule = __decorate([
    (0, common_1.Module)({
        controllers: [bm_controller_1.BmController],
        providers: [bm_service_1.BmService],
        exports: [bm_service_1.BmService],
    })
], BmModule);
//# sourceMappingURL=bm.module.js.map