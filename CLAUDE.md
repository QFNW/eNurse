# eNurse（易护）

患者生理体征追踪护理软件。患者端记录大便/用药打卡并设置提醒策略，护理端管理小组并监控成员状态，微信Bot自动推送警报，Web后台统一管理账号。

## 技术栈

| 层 | 选型 |
|---|---|
| 服务端 | Node.js + NestJS + TypeScript + Prisma + PostgreSQL |
| 队列/缓存 | Redis + Bull Queue |
| 认证 | JWT (access 15min + refresh 7d) |
| Android | Kotlin + Jetpack Compose + Hilt + Room + Retrofit |
| Web后台 | React 18 + Vite + Ant Design 5 |
| 微信Bot | Wechaty + Puppet PadLocal |

## 项目结构

```
eNurse/
├── server/        # NestJS API 服务端
├── android/       # Android App（患者端/护理端同一App，角色分流）
├── web-admin/     # React Web 后台管理
├── docs/          # 详细设计文档
└── needs.txt      # 原始需求文档
```

## 角色

- **PATIENT** — 大便打卡、用药打卡、日历、提醒设置、加入小组
- **NURSE** — 创建/管理小组、查看成员事件/策略/日历、锁定策略
- **ADMIN** — Web后台统一管理所有账号和系统配置

## UI 品牌

- 应用名称：**易护** (eNurse)
- 医疗护理风格，主色调蓝色系
- 警报状态用红色 #D32F2F（加粗大号字体）
- 正常状态蓝色系

## Compose 规则

- **禁止用 if/else 或 AnimatedVisibility 切换 TextField/OutlinedTextField**。切换会导致 Compose 销毁并重建 TextField，输入状态（光标、IME 组合）丢失。始终使用单一 TextField + 动态绑定 `value`/`onValueChange`/`label`。
- **`SnackbarHostState.showSnackbar()` 是 suspend 函数**，suspend 到 snackbar 消失才返回。始终将 `refresh*()` / 状态更新放在 `showSnackbar()` **之前**，否则 UI 会延迟 2-3 秒才刷新。**这是项目中反复出现的 bug，每次新加 snackbar 都必须检查顺序。**
