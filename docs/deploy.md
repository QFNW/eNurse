# eNurse 生产部署指南（宝塔 Linux 面板版）

宝塔方案：**宝塔的 nginx 托管 web-admin 静态页面并反代 API（HTTPS + 证书自动续期全部在宝塔里解决）**；PostgreSQL、Redis、API 服务端跑在 Docker Compose 里。宝塔只当「管理系统 + 反代入口」，不负责跑应用。

## 一、架构

```
云服务器 (Alibaba Cloud Linux 3, x86_64, 2C2G+, 宝塔面板)
├── 宝塔 nginx :443 HTTPS (一键 Let's Encrypt) + :80 → 301
│   ├── /           → /www/wwwroot/enurse  (web-admin vite 产物, SPA)
│   ├── /assets/    → 静态资源 30d 缓存
│   └── /api/*      → 反代 127.0.0.1:3000 (REST API + Swagger /api/docs)
└── docker compose (deploy/docker-compose.prod.yml)
    ├── server 容器   → Node 22 + NestJS, 启动时 prisma migrate deploy, 仅监听 127.0.0.1:3000
    ├── postgres:16   → 命名卷持久化, 不暴露端口
    └── redis:7       → 命名卷持久化, 不暴露端口
```

单域名方案：`https://你的域名` → 管理后台，`https://你的域名/api/v1` → API，`https://你的域名/api/docs` → Swagger。Android App 连 `https://你的域名/api/v1/`。

## 二、服务器规格与成本（预估）

| 项 | 建议 |
|---|---|
| 服务器 | 阿里云/腾讯云**轻量应用服务器 2C2G 起步**（宝塔面板本身约占 100–200MB，见下） |
| 内存 | 2G 够用（Node ~400MB + PG ~300MB + Redis ~100MB + 宝塔 ~200MB + 系统 ~500MB）；**不要装宝塔的 MySQL**（白占 300–500MB）。计划接入微信 Bot 时再升 4G |
| 带宽 | 3–5 Mbps 足够（API 无大文件，反馈无附件） |
| 费用 | 轻量 2C2G 约 ¥30–70/月；2C4G 约 ¥50–100/月（新用户常有折扣） |
| 域名 | 任意注册商域名，**国内机房服务器必须 ICP 备案**（域名 + 服务器同一云厂商流程最简单，约 1–2 周，未备案域名公网访问会被云厂商拦截，手机 App 常表现为 connection reset）。**不想备案直接选海外地域**：腾讯云/阿里云香港、首尔等均免备案，国内延迟 20–100ms，对本 App 无感。**海外地域可复用同一个域名**（A 记录指向海外 IP 即可，证书在宝塔重新申请一次） |
| 证书 | 宝塔面板内 Let's Encrypt 一键申请 + 自动续期（**无需手动管理证书文件**） |

## 三、部署步骤

### 1. 安装宝塔面板

服务器系统为 **Alibaba Cloud Linux 3**（RHEL/CentOS 8 兼容，包管理用 `dnf`）。SSH 登录后按宝塔官网一键脚本安装（`bt.cn`，Alibaba Cloud Linux 走 CentOS 系脚本）：

```bash
curl -sSO https://download.bt.cn/install/install_panel.sh && bash install_panel.sh
```

装完后用浏览器打开面板地址（默认 8888 端口）。

### 2. 安装 Docker

宝塔软件商店有「Docker 管理器」插件（需付费几块钱），或 SSH 命令行安装。**国内服务器走阿里云源，腾讯云服务器走腾讯云源**（get.docker.com 直连可能超时）：

```bash
# 阿里云服务器：阿里云 docker-ce 源
dnf install -y dnf-plugins-core
dnf config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo

# 腾讯云服务器：腾讯云 docker-ce 源（二选一，按服务器厂商）
dnf config-manager --add-repo https://mirrors.cloud.tencent.com/docker-ce/linux/centos/docker-ce.repo

# 安装（共用）
dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
systemctl enable --now docker
docker compose version   # 确认 compose 插件（deploy.sh 依赖）
```

**拉镜像超时**（国内常见）配置镜像加速器：阿里云 → `registry-mirrors` 用阿里云容器镜像服务专属地址；腾讯云 → `{"registry-mirrors": ["https://mirror.ccs.tencentyun.com"]}` 写入 `/etc/docker/daemon.json` 后 `systemctl restart docker`。海外地域一般不用配。

> 宝塔自带 nginx 与 Docker 无关，互不影响。**不要**在宝塔里再装 MySQL（我们用 PostgreSQL 容器）。

### 3. 拉取代码

```bash
git clone https://github.com/QFNW/eNurse.git
cd eNurse
```

### 4. 准备生产环境变量

```bash
cp deploy/.env.prod.example deploy/.env.prod
```

编辑 `deploy/.env.prod`，必须修改：

| 变量 | 生成方式 |
|---|---|
| `POSTGRES_PASSWORD` / `DATABASE_URL` | 自定强密码（两处一致） |
| `ACCESS_TOKEN_SECRET` / `REFRESH_TOKEN_SECRET` | `openssl rand -hex 32` |
| `SMTP_PASS` | 163 邮箱授权码（见 `docs/163-smtp-setup.md`），不是登录密码 |
| `ADMIN_PASSWORD_HASH` | `node -e "const b=require('bcrypt');b.hash('你的管理员密码',12).then(h=>console.log(h))"`（server 目录下运行） |
| `CORS_ORIGIN` | 模板已预填 `https://enurse.ccwu.cc`；换域名才需改 |

> **⚠️ 关键坑：`ADMIN_PASSWORD_HASH` 里每个 `$` 必须写成 `$$`**。bcrypt 哈希形如 `$2b$12$...`，docker compose 的 env 文件会把 `$xxx` 当变量插值，导致哈希被清空、管理员密码失效（启动时 `compose config` 会报 `The "LqH..." variable is not set` 警告）。正确写法：`ADMIN_PASSWORD_HASH=$$2b$$12$$LqH...`。其余变量值里不要出现裸 `$`。
>
> 注意：`NODE_ENV=production` 下 JWT 密钥与管理员哈希缺一不可，否则服务端拒绝启动/管理员无法登录。

### 5. 宝塔创建站点 + 反代配置 + SSL

1. 域名 A 记录解析到服务器公网 IP，等待生效
2. 宝塔 → **网站 → 添加站点**：填域名，PHP 版本选「纯静态」，根目录改为 `/www/wwwroot/enurse`（与 deploy.sh 的 `WEB_ROOT` 一致）
3. 网站 → 设置 → **配置文件**：把 [deploy/nginx/bt-site.conf](deploy/nginx/bt-site.conf) 中注释以外的内容粘贴进 `server { }` 块内，**替换默认的 `location /` 块**。**`location /api/` 必须写成 `location ^~ /api/`**——宝塔默认配置里有 `location ~ .*\.(js|css)?$` 正则缓存规则，nginx 正则优先级高于普通前缀，会把 `/api/docs/swagger-ui-bundle.js` 这类请求劫持为静态文件 404，导致 Swagger 白屏（症状：`/api/docs` 页面空白、直连 127.0.0.1:3000 却 200）。`^~` 让前缀匹配不再回退正则
4. 网站 → SSL → 申请 **Let's Encrypt 证书**（一键，自动续期，自动配 80→443 跳转）

> 证书由宝塔托管，不需要像容器方案那样手动拷贝 `fullchain.pem/privkey.pem`。

### 6. 一键部署

> **前置：服务器需要 Node.js 22 + npm**（deploy.sh 要构建 web-admin）。宝塔 → 软件商店 → 「Node.js 版本管理器」→ 安装 22.x，装完把 `/www/server/nodejs/v22.x.x/bin` 加入 PATH（`echo 'export PATH=/www/server/nodejs/v22.x.x/bin:$PATH' >> ~/.bashrc`）。不装则报 `npm: command not found`。

```bash
bash deploy/scripts/deploy.sh
```

脚本会：校验 env 必填项 → 构建 web-admin（`VITE_API_BASE=/api/v1`）→ 清空并拷贝产物到 `WEB_ROOT`（默认 `/www/wwwroot/enurse`，可用环境变量覆盖）→ compose 配置预检 → `up -d --build`（postgres+redis+server）→ 健康检查（API 回环 401 通过；网页 200 未通过则仅警告，提示检查宝塔站点配置）。

### 7. 验证

```bash
# 网页
curl -I https://你的域名
# API（未认证应返回 401，说明服务在线）
curl -i https://你的域名/api/v1/admin/stats | head -5
# Swagger
打开 https://你的域名/api/docs
```

管理后台登录：`https://你的域名` → 管理员密码 = 你在 `ADMIN_PASSWORD_HASH` 中哈希的那个。

## 四、Android release APK（发布给使用者）

见 `docs/android-release.md`（本地构建，一次完成；产物 `app-release.apk`）。

App 内「我的 → 版本更新」从服务器静态目录检查更新。发布步骤：

1. **构建**：`cd android && ./gradlew :app:assembleRelease`（产物 `android/app/build/outputs/apk/release/app-release.apk`）
2. **上传 APK**：宝塔文件管理 → 站点根目录 `/www/wwwroot/enurse/apk/` 上传 `app-release.apk`（若目录不存在先创建）
3. **更新 version.json**（同目录，版本号与构建一致）：

```json
{
  "versionCode": 3,
  "versionName": "1.0.2",
  "apkUrl": "https://enurse.ccwu.cc/apk/app-release.apk",
  "changelog": "更新说明（可选）"
}
```

> **版本号规则**：发新版时先在 `android/app/build.gradle.kts` 递增 `versionCode`（+1）并同步 `versionName`，再构建；version.json 与之一致。App 侧 `versionCode` 大于本地版本才提示更新。
>
> nginx 无需额外配置（`location /` 的 `try_files` 会直接返回 `/apk/` 下的静态文件）。

4. **验证**：浏览器访问 `https://enurse.ccwu.cc/apk/version.json` 返回 JSON；`https://enurse.ccwu.cc/apk/app-release.apk` 可下载。

> 用户手机安装时需允许「安装未知应用」（系统安装器会自动引导，正常流程）。

## 五、备份

每日凌晨 pg_dump 到服务器磁盘（保留 7 天）：

```bash
sudo crontab -e
# 追加：
0 3 * * * docker exec $(docker ps -qf name=deploy-postgres-1) pg_dump -U enurse enurse | gzip > /root/enurse-backup/enurse_$(date +\%F).sql.gz && find /root/enurse-backup -name '*.sql.gz' -mtime +7 -delete
```

> 容器名以 `docker ps` 实际为准（compose 项目名 `deploy` → 容器 `deploy-postgres-1`）。恢复：`zcat 备份文件 | docker exec -i <postgres容器> psql -U enurse enurse`。

## 六、日常运维

```bash
# 日志
docker compose -f deploy/docker-compose.prod.yml logs -f server
# 重启
docker compose -f deploy/docker-compose.prod.yml restart server
# 升级（拉新代码后重跑 deploy.sh 即可，migrations 自动执行）
git pull && bash deploy/scripts/deploy.sh
```

## 七、注意事项

1. **80/443 归宝塔 nginx 管**：server 容器只监听 `127.0.0.1:3000`（compose 里已限定回环，公网无法直连 API），所有外部流量必须经过宝塔 nginx。**不要**在宝塔里再建监听 80/443 的其他服务
2. **不要装宝塔 MySQL**：2G 内存下 MySQL 会把余量吃光。若已装，在软件商店里停止并设为不启动
3. **站点根目录的 `.user.ini`**：宝塔建站会写入防跨站配置，若页面出现 403，删除站点根目录下的 `.user.ini` 即可
4. **Prisma client 需构建时生成**：`server/generated/` 被 gitignore，镜像构建时自动 `prisma generate`，无需手动操作
5. **启动入口**：容器用 `node dist/src/main.js`（`start:prod` script 指向 `dist/main` 是历史遗留错误，勿用）
6. **Redis 无密码**：compose 内网隔离（不映射宿主端口），如需加固再配置 requirepass（需同步改 `cache.service.ts`）
7. **微信 Bot（Wechaty + PadLocal）尚未实现**：`PADLOCAL_TOKEN` 仅为占位，警报目前只落库不推送；实现后作为 server 的独立进程/模块接入，token 走同一 env
8. **邮件验证码**：SMTP 未配置时降级为控制台日志输出（生产建议保持 163 配置）
9. **CORS**：浏览器同域请求不受 CORS 限制，但 `CORS_ORIGIN` 仍建议设为你的域名（兜底），Android App 为原生请求不受影响
10. **宝塔安全组**：阿里云/腾讯云安全组放行 80/443（22 仅限你自己的 IP 更佳）；宝塔面板端口（默认 8888）勿对公网开放或改随机端口

## 八、容器 nginx 方案（备选）

若不用宝塔 nginx（例如以后迁移到非宝塔服务器），可退回 `docker-compose.prod.yml` 的 nginx 容器方案：恢复部署资产中注释掉的 nginx 服务（见 git 历史中 `deploy/nginx/nginx.conf` + `deploy/nginx/certs/` 用法），deploy.sh 相应改回直挂 `web-admin/dist` 卷。
