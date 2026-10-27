# eNurse 生产部署指南（雨云香港 + 宝塔 Debian 12）

当前方案：**雨云香港 2C2G（Debian 12，免备案）+ 宝塔面板**。宝塔的 nginx 托管 web-admin 静态页面并反代 API（HTTPS + 证书自动续期在宝塔内完成）；PostgreSQL、Redis、API 服务端跑在 Docker Compose 里。宝塔只当「管理系统 + 反代入口」，不负责跑应用。

生产域名：`bohack26.kdns.fr`（KataBump 免费法国二级域名）。

## 一、架构

```text
雨云香港 2C2G (Debian 12, x86_64, 宝塔面板)
├── 宝塔 nginx :443 HTTPS (一键 Let's Encrypt) + :80 → 301
│   ├── /           → /www/wwwroot/enurse  (web-admin vite 产物, SPA)
│   ├── /assets/    → 静态资源 30d 缓存
│   ├── /apk/       → APK 分发目录（deploy.sh 会保留，不会被清空）
│   └── /api/*      → 反代 127.0.0.1:3000 (REST API + Swagger /api/docs)
└── docker compose (deploy/docker-compose.prod.yml)
    ├── server 容器   → Node 22 + NestJS, 启动时 prisma migrate deploy, 仅监听 127.0.0.1:3000
    ├── postgres:16   → 命名卷持久化, 不暴露端口
    └── redis:7       → 命名卷持久化, 不暴露端口
```

单域名方案：`https://bohack26.kdns.fr` → 管理后台，`https://bohack26.kdns.fr/api/v1` → API，`https://bohack26.kdns.fr/api/docs` → Swagger。Android App 连 `https://bohack26.kdns.fr/api/v1/`。

## 二、服务器与环境（当前方案）

| 项 | 当前方案 |
| --- | --- |
| 服务器 | **雨云 香港 2C2G**（免备案，国内访问延迟通常 20–100ms） |
| 系统 | **Debian 12 (bookworm), x86_64** |
| 面板 | 宝塔 Linux 面板（Debian 12 版）+ Nginx |
| 环境 | 选 LNMP 时**只保留 Nginx**；MySQL/PHP 不需要（数据库用 Docker 里的 PostgreSQL） |
| 域名 | `bohack26.kdns.fr`（KataBump 免费二级域名；`kdns.fr` 在 Public Suffix List 中，可独立申请 Let's Encrypt） |
| 证书 | 宝塔内 Let's Encrypt 一键申请 + 自动续期（无需手动管理证书文件） |
| 内存 | 2G 够用：Node ~400MB + PG ~300MB + Redis ~100MB + 宝塔 ~200MB + 系统 ~500MB。**不要跑 MySQL**（白占 300–500MB） |
| 带宽 | 3–5 Mbps 足够（API 无大文件，反馈无附件） |

> 香港服务器不需要 ICP 备案，域名解析生效即可公网访问。若以后迁回大陆机房，域名需先备案。

## 三、部署步骤

### 1. 安装宝塔面板

服务器系统为 **Debian 12**。SSH 登录后执行宝塔官网的 Linux 一键脚本（脚本会自动识别 Debian）：

```bash
apt-get update
apt-get install -y wget curl
wget -O install.sh https://download.bt.cn/install/install_lts.sh
bash install.sh
```

> 安装命令以 [bt.cn](https://www.bt.cn) 官网当前 Debian 12 页面为准。装完后用浏览器打开面板地址（默认 8888 端口），按提示安装 Nginx（LNMP 环境里 **MySQL 和 PHP 都可以不装**）。

### 2. 安装 Docker（Debian 12）

宝塔软件商店有「Docker 管理器」插件，也可以直接用 Docker 官方 Debian 源安装：

```bash
apt-get update
apt-get install -y ca-certificates curl gnupg
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo "$VERSION_CODENAME") stable" > /etc/apt/sources.list.d/docker.list
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable --now docker
docker compose version   # 确认 compose 插件（deploy.sh 依赖）
```

**拉镜像超时**（国内网络偶尔出现）：在 `/etc/docker/daemon.json` 配置 `registry-mirrors` 后 `systemctl restart docker`；香港机器直连 Docker Hub 通常可用。也可直接用宝塔的 Docker 管理器拉取 `node:22-alpine`、`postgres:16-alpine`、`redis:7-alpine`。

> 宝塔自带 nginx 与 Docker 无关，互不影响。**不要**在宝塔里再装 MySQL（我们用 PostgreSQL 容器）。如果 LNMP 已经装了 MySQL，在软件商店里停止并设为不启动。

### 3. 拉取代码

```bash
git clone https://github.com/QFNW/eNurse.git
cd eNurse
```

### 4. 准备生产环境变量

#### 4.1 复制模板

```bash
cp deploy/.env.prod.example deploy/.env.prod
chmod 600 deploy/.env.prod
```

`deploy/.env.prod` 已在 `deploy/.gitignore` 中，**不要提交到 git**。下面所有值都只存在这台服务器上。

#### 4.2 逐项填写

用 `vim deploy/.env.prod` 或宝塔文件管理器编辑。建议先用 `openssl rand -hex` 生成随机值，避免 `@ : / $` 等特殊字符带来的转义问题。

> `openssl rand -hex N` 的 `N` 是**随机字节数**，输出长度是 `2N` 个十六进制字符：`-hex 16` → 32 位，`-hex 32` → 64 位。所以数据库密码用 16 字节（128 位熵）、JWT 密钥用 32 字节（256 位熵）。

| 变量 | 填写内容 | 说明 |
| --- | --- | --- |
| `NODE_ENV` | `production` | 保持模板值。生产模式下缺少 JWT 密钥或管理员哈希会拒绝启动 |
| `POSTGRES_USER` | `enurse` | 保持默认即可 |
| `POSTGRES_DB` | `enurse` | 保持默认即可 |
| `POSTGRES_PASSWORD` | `openssl rand -hex 16` 的输出 | 数据库密码，**必须与 `DATABASE_URL` 里的密码完全一致** |
| `DATABASE_URL` | `postgresql://enurse:<同一密码>@postgres:5432/enurse?schema=public` | 主机名必须是 `postgres`（compose 服务名），不能写 `localhost` |
| `ACCESS_TOKEN_SECRET` | `openssl rand -hex 32` | 访问令牌密钥，单独生成一组 |
| `ACCESS_TOKEN_EXPIRES_IN` | `15m` | 保持默认 |
| `REFRESH_TOKEN_SECRET` | `openssl rand -hex 32` | 刷新令牌密钥，**必须与 access 的不同** |
| `REFRESH_TOKEN_EXPIRES_IN` | `7d` | 保持默认 |
| `REDIS_HOST` | `redis` | compose 服务名，不能写 `localhost` |
| `REDIS_PORT` | `6379` | 保持默认 |
| `SMTP_HOST` | `smtp.163.com` | 163 邮箱的 SMTP 地址 |
| `SMTP_PORT` | `465` | 465 为 SSL 端口 |
| `SMTP_USER` | 你的完整 163 邮箱地址 | 例如 `yourname@163.com`，模板里是示例地址，要改 |
| `SMTP_PASS` | 163 邮箱的**授权码** | 在 163 邮箱「设置 → POP3/SMTP/IMAP」中开启服务后获取，不是登录密码。详见 `docs/163-smtp-setup.md`。留空则验证码打印到服务端日志 |
| `ADMIN_PASSWORD_HASH` | 管理员密码的 bcrypt 哈希 | 生成方法见 4.3，**注意 `$` 要写成 `$$`** |
| `PORT` | `3000` | 容器内部端口，由宝塔 nginx 反代 |
| `CORS_ORIGIN` | `https://bohack26.kdns.fr` | 模板已预填；换域名才需改 |
| `TZ` | `Asia/Shanghai` | 影响告警引擎按 `reminderTime` 匹配时刻，香港机器默认 UTC，**不要删** |

#### 4.3 生成管理员密码哈希

在 `server/` 目录下执行（`bcrypt` 是服务端依赖）：

```bash
cd server
npm install    # 首次部署需要，确保 bcrypt 已安装
node -e "const b=require('bcrypt');b.hash('替换成你的管理员密码',12).then(h=>console.log(h))"
cd ..
```

输出类似：

```text
$2b$12$LqH8xY...（省略）
```

然后把**每一个 `$` 都替换成 `$$`** 再写入 `.env.prod`：

```dotenv
ADMIN_PASSWORD_HASH=$$2b$$12$$LqH8xY...（省略）
```

> **为什么必须写 `$$`**：docker compose 读取 env 文件时会做变量插值，`$2b$12$...` 里的 `$2b`、`$12` 会被当成变量名，导致哈希被清空，管理员无法登录。启动时如果看到 `The "LqH..." variable is not set` 警告，就是这里没转义。
>
> 哈希值是跨机器通用的：如果服务器还没装 Node 22（见第 6 步），可以先在本地生成后粘贴过来，不必为了生成哈希提前装 Node。不要给哈希加引号，`$$` 才是正确做法。

#### 4.4 填写后的关键项示例（脱敏）

```dotenv
NODE_ENV=production
POSTGRES_USER=enurse
POSTGRES_DB=enurse
POSTGRES_PASSWORD=<32位随机hex>
DATABASE_URL=postgresql://enurse:<同一32位随机hex>@postgres:5432/enurse?schema=public

ACCESS_TOKEN_SECRET=<64位随机hex>
REFRESH_TOKEN_SECRET=<另一组64位随机hex>

REDIS_HOST=redis
REDIS_PORT=6379

SMTP_HOST=smtp.163.com
SMTP_PORT=465
SMTP_USER=yourname@163.com
SMTP_PASS=<163授权码>

ADMIN_PASSWORD_HASH=$$2b$$12$$<哈希剩余部分>
CORS_ORIGIN=https://bohack26.kdns.fr
TZ=Asia/Shanghai
```

#### 4.5 部署前自检

```bash
# 1) 必填项都已填写（不应出现 CHANGE_ME / 空值）
grep -E '^(POSTGRES_PASSWORD|DATABASE_URL|ACCESS_TOKEN_SECRET|REFRESH_TOKEN_SECRET|ADMIN_PASSWORD_HASH|CORS_ORIGIN|TZ)=' deploy/.env.prod

# 2) compose 能正确解析该文件（无输出即通过）
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.prod config -q
```

> `deploy.sh` 也会在构建前检查 `ACCESS_TOKEN_SECRET`、`REFRESH_TOKEN_SECRET`、`ADMIN_PASSWORD_HASH` 三项，缺失会直接退出。

#### 4.6 常见错误

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 管理员登录报密码错误 | `ADMIN_PASSWORD_HASH` 的 `$` 没写成 `$$`，哈希被插值清空 | 重新生成并按 4.3 转义 |
| 服务启动报数据库认证失败 | `POSTGRES_PASSWORD` 与 `DATABASE_URL` 中的密码不一致，或密码含 `@ : /` 未 URL 编码 | 两处改成同一个纯 hex 密码 |
| 收不到邮箱验证码 | `SMTP_PASS` 填了登录密码而不是授权码 | 重新获取 163 授权码；或先留空，从 `docker compose ... logs -f server` 看验证码 |
| 日志报 Redis 连接失败 | `REDIS_HOST` 写成了 `localhost` | 改为 compose 服务名 `redis` |
| 修改了 `POSTGRES_PASSWORD` 但数据库仍用旧密码 | 数据卷已用首次密码初始化，改 env 不会改数据库 | 进入容器 `ALTER USER enurse WITH PASSWORD '新密码';`，或删除卷重建（**会清空数据**） |

### 5. 配置 DNS + 宝塔站点 + SSL

1. **DNS**：在 KataBump 面板（dashboard.katabump.com）把 `bohack26.kdns.fr` 的 **A 记录**指向雨云香港服务器的公网 IP，等待生效（`nslookup bohack26.kdns.fr` 应返回该 IP）。
   - 如果套 Cloudflare：签发证书期间先用**灰云（DNS only）**，否则 Let's Encrypt 的 HTTP-01 校验可能失败；拿到证书后再决定是否开代理。
2. 宝塔 → **网站 → 添加站点**：域名填 `bohack26.kdns.fr`，PHP 版本选「纯静态」，根目录改为 `/www/wwwroot/enurse`（与 deploy.sh 的 `WEB_ROOT` 一致）。
3. 网站 → 设置 → **配置文件**：把 [deploy/nginx/bt-site.conf](../deploy/nginx/bt-site.conf) 中注释以外的内容粘贴进 `server { }` 块内，**替换默认的 `location /` 块**。`location ^~ /api/` 中的 `^~` 必须保留——宝塔默认配置里有 `location ~ .*\.(js|css)?$` 正则缓存规则，nginx 正则优先级高于普通前缀，会把 `/api/docs/swagger-ui-bundle.js` 这类请求劫持为静态文件 404，导致 Swagger 白屏（症状：`/api/docs` 页面空白、直连 `127.0.0.1:3000` 却 200）。
4. 网站 → SSL → 申请 **Let's Encrypt 证书**（一键，自动续期，自动配 80→443 跳转）。

> 证书由宝塔托管，不需要像容器方案那样手动拷贝 `fullchain.pem/privkey.pem`。
>
> `kdns.fr` 在 Public Suffix List 中，`bohack26.kdns.fr` 会按独立域名计算 Let's Encrypt 速率限制，不受其他用户影响。

### 6. 一键部署

> **前置：服务器需要 Node.js 22 + npm**（deploy.sh 要构建 web-admin，Vite 8 要求 Node 22.12+）。宝塔 → 软件商店 → 「Node.js 版本管理器」→ 安装 22.x，装完把 `/www/server/nodejs/v22.x.x/bin` 加入 PATH：
>
> ```bash
> echo 'export PATH=/www/server/nodejs/v22.x.x/bin:$PATH' >> ~/.bashrc
> source ~/.bashrc
> node -v   # 应为 v22.x
> ```
>
> 不装则报 `npm: command not found`。

```bash
bash deploy/scripts/deploy.sh
```

脚本会：校验 env 必填项 → 构建 web-admin（`VITE_API_BASE=/api/v1`）→ 保留站点根目录下的 `apk/` → 清空并拷贝产物到 `WEB_ROOT`（默认 `/www/wwwroot/enurse`，可用环境变量覆盖）→ compose 配置预检 → `up -d --build`（postgres+redis+server）→ 健康检查（API 回环 401 通过；网页 200 未通过则仅警告，提示检查宝塔站点配置）。

### 7. 验证

```bash
# 网页
curl -I https://bohack26.kdns.fr
# API（未认证应返回 401，说明服务在线）
curl -i https://bohack26.kdns.fr/api/v1/admin/stats | head -5
# Swagger
打开 https://bohack26.kdns.fr/api/docs
```

管理后台登录：`https://bohack26.kdns.fr` → 管理员密码 = 你在 `ADMIN_PASSWORD_HASH` 中哈希的那个。

## 四、Android release APK（发布给使用者）

见 `docs/android-release.md`（本地构建，一次完成；产物 `app-release.apk`）。

App 内「我的 → 版本更新」从服务器静态目录检查更新。发布步骤：

1. **确认域名可用**：先保证 `https://bohack26.kdns.fr` 与 API 已经正常，再发布 APK。
2. **构建**：`cd android && ./gradlew :app:assembleRelease`（产物 `android/app/build/outputs/apk/release/app-release.apk`）。release 的 API 地址来自 `android/local.properties` 的 `apiBaseUrl`，已指向 `https://bohack26.kdns.fr/api/v1/`。
3. **上传 APK**：宝塔文件管理 → 站点根目录 `/www/wwwroot/enurse/apk/` 上传 `app-release.apk`（若目录不存在先创建）。`deploy.sh` 会保留这个目录，后续重新部署不会删除。
4. **更新 version.json**（同目录，版本号与构建一致）：

```json
{
  "versionCode": 5,
  "versionName": "1.0.4",
  "apkUrl": "https://bohack26.kdns.fr/apk/app-release.apk",
  "changelog": "更新说明（可选）"
}
```

> **版本号规则**：发新版时先在 `android/app/build.gradle.kts` 递增 `versionCode`（+1）并同步 `versionName`，再构建；version.json 与之一致。App 侧 `versionCode` 大于本地版本才提示更新。如果此前没有分发过旧包，可以沿用当前版本号。
>
> nginx 无需额外配置（`location /` 的 `try_files` 会直接返回 `/apk/` 下的静态文件）。

**验证**：浏览器访问 `https://bohack26.kdns.fr/apk/version.json` 返回 JSON；`https://bohack26.kdns.fr/apk/app-release.apk` 可下载。

> 用户手机安装时需允许「安装未知应用」（系统安装器会自动引导，正常流程）。

## 五、备份

每日凌晨 pg_dump 到服务器磁盘（保留 7 天）：

```bash
sudo crontab -e
# 追加：
0 3 * * * docker exec $(docker ps -qf name=deploy-postgres-1) pg_dump -U enurse enurse | gzip > /root/enurse-backup/enurse_$(date +\%F).sql.gz && find /root/enurse-backup -name '*.sql.gz' -mtime +7 -delete
```

> 容器名以 `docker ps` 实际为准（compose 项目名 `deploy` → 容器 `deploy-postgres-1`）。恢复：`zcat 备份文件 | docker exec -i <postgres容器> psql -U enurse enurse`。
>
> 雨云控制台如有快照功能，建议同时开每日自动快照，覆盖整机故障场景。

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

1. **80/443 归宝塔 nginx 管**：server 容器只监听 `127.0.0.1:3000`（compose 里已限定回环，公网无法直连 API），所有外部流量必须经过宝塔 nginx。**不要**在宝塔里再建监听 80/443 的其他服务。
2. **LNMP 只留 Nginx**：PostgreSQL 跑在 Docker 里，宝塔的 MySQL 不要装；已装则停止并设为不启动。PHP 不参与本项目，可停用。
3. **站点根目录的 `.user.ini`**：宝塔建站会写入防跨站配置，若页面出现 403，删除站点根目录下的 `.user.ini` 即可。
4. **Prisma client 需构建时生成**：`server/generated/` 被 gitignore，镜像构建时自动 `prisma generate`，无需手动操作。
5. **启动入口**：容器用 `node dist/src/main.js`（`start:prod` script 指向 `dist/main` 是历史遗留错误，勿用）。
6. **Redis 无密码**：compose 内网隔离（不映射宿主端口），如需加固再配置 requirepass（需同步改 `cache.service.ts`）。
7. **微信 Bot（Wechaty + PadLocal）尚未实现**：`PADLOCAL_TOKEN` 仅为占位，警报目前只落库并推送到 App；实现后作为 server 的独立进程/模块接入，token 走同一 env。
8. **邮件验证码**：SMTP 未配置时降级为控制台日志输出（`docker compose logs -f server` 查看）；正式使用建议配置 163 SMTP。
9. **CORS**：浏览器同域请求不受 CORS 限制，但 `CORS_ORIGIN` 仍建议设为 `https://bohack26.kdns.fr`（兜底），Android App 为原生请求不受影响。
10. **雨云防火墙/安全组**：放行 80/443；22 端口尽量只允许自己的 IP；**不要**放行 3000/5432/6379。宝塔面板端口（默认 8888）改随机端口或限制来源 IP。
11. **Debian 12 无 SELinux**；若自行启用了 `ufw`，需放行 80/443 以及本机回环访问 3000（容器映射）。
12. **时区**：`.env.prod` 保持 `TZ=Asia/Shanghai`。告警引擎用服务端本地时间匹配 `reminderTime`，香港机器默认 UTC 会导致提醒偏移 8 小时。

## 八、容器 nginx 方案（备选）

若不用宝塔 nginx（例如以后迁移到非宝塔服务器），可退回 `docker-compose.prod.yml` 的 nginx 容器方案：恢复部署资产中注释掉的 nginx 服务（见 git 历史中 `deploy/nginx/nginx.conf` + `deploy/nginx/certs/` 用法），deploy.sh 相应改回直挂 `web-admin/dist` 卷。
