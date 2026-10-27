# 易护 eNurse

患者生理体征追踪护理软件：患者记录大便与用药、设置提醒策略，护理人员按小组监控成员状态，Web 后台统一管理账号与数据。

同一个 Android App 根据账号角色进入患者端或护理端。

## 角色

| 角色 | 能力 |
| --- | --- |
| PATIENT | 大便打卡、用药打卡、大便日历、提醒策略、加入小组 |
| NURSE | 创建/管理小组、邀请码拉人、查看成员事件与策略、编辑/锁定策略、代打卡 |
| ADMIN | Web 后台：数据概览、用户管理、小组管理、反馈管理 |

## 功能

### 患者端（Android）

- 大便打卡与用药打卡，支持多药品、备注、离线打卡（联网后自动补传）
- 大便日历，有记录的日期标红，可查看当天详情
- 提醒策略：大便按“间隔 N 天 + 提醒时刻”，用药支持固定时刻与间隔两种方式
- 主页红色加粗大号字体展示未解决警报
- 双通道提醒：服务端 30 秒轮询 + 本地 `AlarmManager` 精确闹钟，开机与升级后自动重建
- 应用内更新：读取 `/apk/version.json`，下载新 APK 并拉起安装器

### 护理端（Android）

- 创建多个小组，通过邀请码邀请患者（每个患者最多加入一个小组）
- 查看成员的事件（警报/大便/用药）、策略与大便日历
- 编辑成员策略、锁定/解锁策略、代打卡、创建虚拟患者
- 修改策略后引导开启系统精确闹钟权限

### 管理后台（Web）

- 数据概览：用户数、患者/护士数、小组数、打卡统计
- 用户管理（含重置密码）、用户详情、小组管理、问题反馈查看

### 告警引擎

- 大便每 5 分钟扫描，用药每 1 分钟扫描，每天 3:00 清理陈旧告警
- 同一事项只保留最新一条未解决告警，新告警产生时自动解决旧告警
- 告警 fan-out 给患者所在小组的所有护理人员，消息带患者姓名
- 未解决告警由 App 每 30 秒轮询获取

## 系统架构

```text
Android App ──REST /api/v1──> NestJS ──Prisma──> PostgreSQL 16
                                   └──ioredis──> Redis 7
Web Admin ────/api/v1/admin──> NestJS
Nginx ── 静态资源（web-admin 构建产物、APK）+ /api/ 反向代理
```

## 技术栈

| 层 | 选型 |
| --- | --- |
| 服务端 | NestJS 11 + TypeScript + Prisma 7（pg adapter）+ PostgreSQL 16 |
| 缓存 | Redis 7（ioredis，用于 refresh token 黑名单与邮箱验证码） |
| 认证 | JWT（access 15 分钟 / refresh 7 天）+ bcrypt |
| Android | Kotlin + Jetpack Compose + Hilt + Room + Retrofit/OkHttp（minSdk 26 / target 36） |
| Web 后台 | React 19 + Vite 8 + Ant Design 6 + TanStack Query + axios |
| 部署 | Docker Compose + 宝塔 Nginx + Let's Encrypt |

## 目录结构

```text
eNurse/
├── server/        # NestJS API（Prisma schema、迁移、告警引擎）
├── android/       # Android App，患者端/护理端共用一个 App
├── web-admin/     # React 管理后台
├── deploy/        # Docker Compose、Nginx 配置、部署脚本、环境变量模板
├── docs/          # 部署、API、数据库、Android 发版文档
├── apk/           # 待分发的 APK 与 version.json
└── needs.txt      # 原始需求
```

## 快速开始

### 环境要求

- Node.js 22.12+（Vite 8 要求）
- npm 10+
- Docker 与 Docker Compose（本地跑 PostgreSQL / Redis）
- 构建 Android 额外需要 JDK 17 或 21、Android SDK 36

### 服务端

```bash
cd server
npm install
docker compose up -d          # 启动本地 PostgreSQL 16 + Redis 7
cp .env.example .env          # 首次运行时
npx prisma generate
npx prisma migrate dev
npx prisma db seed
npm run start:dev
```

- API：`http://localhost:3000/api/v1`
- Swagger：`http://localhost:3000/api/docs`
- 未配置 SMTP 时，注册所需的邮箱验证码会打印在服务端控制台
- `db seed` 创建 `admin@enurse.com`，密码取环境变量 `ADMIN_PASSWORD`

### Web 管理后台

```bash
cd web-admin
npm install
npm run dev
```

访问 `http://localhost:5173`，默认请求 `http://localhost:3000/api/v1`。开发环境的管理员密码对应服务端 `ADMIN_PASSWORD`。

### Android

在 `android/local.properties`（已 gitignore）中配置 SDK 与 release 签名：

```properties
sdk.dir=/path/to/Android/Sdk
apiBaseUrl=https://your-domain/api/v1/

keystore.path=app/enurse-release.keystore
keystore.storePassword=...
keystore.alias=enurse
keystore.keyPassword=...
```

```bash
cd android
./gradlew assembleDebug       # Windows: gradlew.bat assembleDebug
```

- debug 包固定请求 `http://10.0.2.2:3000/api/v1/`（模拟器访问宿主机）
- release 包的 API 地址来自 `local.properties` 的 `apiBaseUrl`

## 生产部署

完整步骤见 [docs/deploy.md](docs/deploy.md)，概要：

1. Docker Compose 启动 `postgres` + `redis` + `server`，server 只监听 `127.0.0.1:3000`
2. 宝塔 Nginx 托管 web-admin 静态文件，并将 `/api/` 反向代理到 `127.0.0.1:3000`
3. `deploy/.env.prod` 填写生产密钥；`ADMIN_PASSWORD_HASH` 中的 `$` 需写成 `$$`
4. 执行一键部署：

```bash
bash deploy/scripts/deploy.sh
```

APK 发布流程见 [docs/android-release.md](docs/android-release.md)：构建 `assembleRelease` 后，将 `app-release.apk` 与 `version.json` 上传到站点 `/apk/` 目录。

## 文档

| 文档 | 内容 |
| --- | --- |
| [docs/user-guide.md](docs/user-guide.md) | 患者端与护理端使用说明 |
| [docs/deploy.md](docs/deploy.md) | 服务器部署、SSL、备份与运维 |
| [docs/android-release.md](docs/android-release.md) | Android 签名、打包与发布 |
| [docs/api.md](docs/api.md) | API 端点与请求约定 |
| [docs/database.md](docs/database.md) | 数据模型与关系 |
| [docs/163-smtp-setup.md](docs/163-smtp-setup.md) | 163 邮箱 SMTP 配置 |
| [CLAUDE.md](CLAUDE.md) | 代码约定与架构要点 |

## 已知限制

- 微信 Bot（患者私聊提醒、小组群 @所有人）尚未实现，`needs.txt` 中的该部分仅为规划；当前警报只落库并由 App 轮询展示。
- 生产环境未配置 SMTP 时无法完成注册，必须配置邮件服务；开发环境验证码打印在服务端控制台。



以上内容以及剩下的所有项目文件均由大肥鱼制作。

后台登录地址https://bohack26.kdns.fr/admin，密码与微信群里发的服务器密码相同。

app下载试用页https://bohack26.kdns.fr/apk/app-release.apk
