# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## eNurse（易护）

患者生理体征追踪护理软件：患者端记录大便/用药打卡并设定提醒策略，护理端按小组监控成员事件与策略，Web 后台统一管理账号。

> 微信 Bot（Wechaty + PadLocal）只存在于 `needs.txt` 和 `PADLOCAL_TOKEN` 占位中，**尚未实现**。警报目前只落库并推送到客户端，不要假设有微信推送链路。

## 技术栈（以代码为准）

| 层 | 实际选型 |
|---|---|
| 服务端 | NestJS 11 + TypeScript + Prisma 7（pg adapter）+ PostgreSQL 16 |
| 缓存 | Redis 7（ioredis；仅用于 refresh token 黑名单与邮箱验证码，**没有** Bull 队列） |
| 认证 | JWT access 15min + refresh 7d，bcrypt cost 12 |
| Android | Kotlin + Jetpack Compose + Hilt + Room + Retrofit/OkHttp（minSdk 26 / target 36） |
| Web 后台 | React 19 + Vite 8 + Ant Design 6 + TanStack Query + axios |
| 部署 | Docker Compose（server + postgres + redis）+ 宝塔 nginx |

## 常用命令

### 服务端（`server/`）

```bash
npm install
docker compose up -d          # 本地 PostgreSQL 16 + Redis 7（该 compose 不含应用）
cp .env.example .env          # 首次
npx prisma generate
npx prisma migrate dev
npx prisma db seed            # 创建 admin@enurse.com
npm run start:dev             # watch 模式，http://localhost:3000，Swagger 在 /api/docs
npm run build                 # nest build + patch-prisma.js
npm run lint
npm test                      # 单元测试（src/**/*.spec.ts，目前没有）
npx jest --config test/jest-e2e.json --testPathPattern="auth"   # 单个 e2e，需服务已在 3000 运行
```

- `npm run build` 必须包含 `patch-prisma.js`（Prisma v7 生成的 ESM 代码在 CJS 产物里的补丁）。单独跑 `nest build` 可能导致运行时报错。
- 生产入口是 `node dist/src/main.js`；`package.json` 的 `start:prod` 指向 `dist/main` 是历史遗留错误，勿用。
- e2e 用例直接请求 `http://localhost:3000`，且早于邮箱验证码改造（注册未传 `code`、登录字段用的是 `email`），**当前大概率跑不通**；改认证相关代码时不要以 e2e 通过为准。

### Android（`android/`）

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew testDebugUnitTest
./gradlew testDebugUnitTest --tests "com.enurse.app.reminder.ReminderTimeCalculatorTest"
```

- debug 的 API 固定为 `http://10.0.2.2:3000/api/v1/`。
- release 从 `android/local.properties` 读取签名与 `apiBaseUrl`（均 gitignored）；`assembleRelease` 需要 keystore，不要随意重建（丢失将无法升级已发布 App）。

### Web 后台（`web-admin/`）

```bash
npm install
npm run dev       # http://localhost:5173，API 默认 http://localhost:3000/api/v1
npm run build     # tsc -b && vite build
npm run lint      # oxlint
```

生产构建用 `VITE_API_BASE=/api/v1 npm run build`（同域部署）。

### 部署与发版

```bash
bash deploy/scripts/deploy.sh   # 构建 web-admin → 拷贝到站点根目录 → compose up → 健康检查
./gradlew assembleRelease       # 然后上传 apk/app-release.apk + version.json 到服务器 /apk/
```

## 系统架构

```
Android App ──REST /api/v1──> NestJS ──Prisma──> PostgreSQL
                                    └─ioredis──> Redis
Web Admin ──/api/v1/admin──> NestJS
静态资源：/apk/version.json + app-release.apk 由 nginx 直接返回
```

### 服务端请求管线

- **路由前缀是手写的**：没有 `setGlobalPrefix`，每个 controller 必须写成 `@Controller('api/v1/<module>')`，新增模块时别漏。
- 全局 `JwtAuthGuard` 默认拦截所有接口，公开接口用 `@Public()`；角色限制用 `@Roles()` + `RolesGuard`（PATIENT / NURSE / ADMIN）。
- 全局 `ValidationPipe({ whitelist: true, forbidNonWhitelisted: true, transform: true })`：DTO 未声明的字段会直接 400。
- 全局 `ChineseExceptionFilter` 输出中文错误；`DateOffsetInterceptor` 把响应里的所有 `Date` 转成 `+08:00` 的 ISO 字符串（存储仍是 UTC）。

### 认证要点

- 注册需邮箱验证码（Redis TTL 5 分钟；SMTP 未配置时验证码打印到服务端控制台）。登录支持用户名或邮箱。
- refresh 轮换：新 token 签发后把旧 `tokenId` 写入 Redis 黑名单。Redis 不可用时黑名单 fail-open，不影响主流程。
- 管理员登录独立于 `users` 表：校验 `ADMIN_PASSWORD_HASH`，签发 `sub=admin` 的 2 小时 JWT。
- 注册 DTO 接受 `role` 且服务层直接写入（包括 ADMIN）；e2e 里的“强制 PATIENT”断言与实现不一致，修改认证时注意。

### 告警引擎（`alerts/alert-engine.service.ts`）

- Cron：大便每 5 分钟扫描，用药每 1 分钟扫描，每天 3:00 清理陈旧告警（BM > 7 天、用药 > 2 天）。
- 大便间隔语义：`intervalDays=N` 表示最近 N-1 天无记录才提醒，只在 `reminderTime`（默认 08:00，±4 分钟）触发。
- 用药：`FIXED_TIME` 按固定时刻（±1 分钟）；`INTERVAL` 从 `startTime` 起算取模（1 分钟窗口）。最近 1 小时内有打卡则跳过。
- **同一事项只保留最新一条**：创建新告警前先 resolve 同 `(userId, type, relatedId)` 的旧未解决告警。患者 BM 的 `relatedId=null`；护士端的 BM 副本用 `patientId`，用药副本用 `medicationId`。
- 告警会 fan-out 给患者所在小组的所有 NURSE（跨组去重），消息前缀带患者姓名。

### Android 关键链路

- 单 Activity + Compose；启动目的地由 `SplashViewModel` 根据 token/角色决定，`AppNavGraph` 再分 `PatientNavGraph`（首页/我的）与 `NurseNavGraph`（小组/我的）。
- `eNurseApp` 监听 `TokenDataStore.accessToken`：有 token 启动 `AlertPollService` + `ReminderScheduler`，登出则全部停止/取消。
- **双通道提醒**：服务端告警 30 秒轮询（`AlertPollService`）+ 本地 `AlarmManager.setExactAndAllowWhileIdle`（`ReminderScheduler`）。`AlarmReceiver` 触发时先查 Room 是否已打卡，未打卡则发高优先级通知并拉起全屏 `ReminderActivity`，随后重排下一次；`BootReceiver` 在开机/应用升级后重建闹钟。本地闹钟刚响过 60 秒内会抑制服务端同类型通知，避免双响。
- `AuthInterceptor` 处理 401：单飞（Mutex + Deferred）刷新 token 后重放原请求；刷新失败清空 token，由 NavGraph 跳登录页。
- 离线打卡：Room 落库 + `pending_sync` 队列，联网后重放并删除队列项。
- 应用内更新：`UpdateChecker` 拉取 `/apk/version.json`，比较 `versionCode`，流式下载 APK 后经 FileProvider 拉起安装器。

### Web 后台

- 路由在 `/admin/*`（登录 `/admin/login`），`ProtectedRoute` 校验 `localStorage.adminToken`。
- axios `baseURL` 取 `VITE_API_BASE`，缺省 `http://localhost:3000/api/v1`；401 自动清 token 并跳登录。

## 部署注意

- 生产 compose 只跑 postgres + redis + server；宿主 nginx 负责 HTTPS 与静态站，server 仅监听 `127.0.0.1:3000`（不要暴露公网）。
- 容器启动时自动 `prisma migrate deploy`，入口为 `node dist/src/main.js`。
- `deploy/.env.prod` 中 `ADMIN_PASSWORD_HASH` 的每个 `$` 必须写成 `$$`，否则 compose 会做变量插值导致哈希失效。
- `/api/` 在宝塔 nginx 中必须用 `location ^~ /api/`，否则正则静态规则会劫持 Swagger 的 JS 文件。
- `TZ=Asia/Shanghai` 影响告警引擎的 `getHours()/getMinutes()`；缺失会导致提醒时间偏移。
- `prisma/migrations/manual_patient_single_group.sql` 是**游离 SQL**（不是时间戳迁移），`prisma migrate deploy` 不会执行。新库如需“每个患者最多一个小组”的数据库级约束，要手动应用；应用层已有 Serializable 事务校验。

## UI 品牌

- 应用名称：**易护** (eNurse)
- 医疗护理风格，主色调蓝色系
- 警报状态用红色 #D32F2F（加粗大号字体），正常状态蓝色系

## Compose 规则

- **禁止用 if/else 或 AnimatedVisibility 切换 TextField/OutlinedTextField**。切换会导致 Compose 销毁并重建 TextField，输入状态（光标、IME 组合）丢失。始终使用单一 TextField + 动态绑定 `value`/`onValueChange`/`label`。
- **`SnackbarHostState.showSnackbar()` 是 suspend 函数**，suspend 到 snackbar 消失才返回。始终将 `refresh*()` / 状态更新放在 `showSnackbar()` **之前**，否则 UI 会延迟 2-3 秒才刷新。**这是项目中反复出现的 bug，每次新加 snackbar 都必须检查顺序。**
