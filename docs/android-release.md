# eNurse Android release 构建指南

本机构建已签名 APK，供使用者直接安装。

## 前置（已完成一次，无需重复）

- **keystore**：`android/app/enurse-release.keystore`（已生成，**不进 git**，妥善保管——丢失则无法升级已有安装的 App）
- **签名配置**：`android/local.properties`（已写入口令，**不进 git**）

## 构建步骤

### 1. 设置生产 API 地址

编辑 `android/local.properties`，追加一行（换成真实域名）：

```properties
apiBaseUrl=https://你的域名/api/v1/
```

> release 构建读取此项；不设置时使用占位地址 `https://bohack26.kdns.fr/api/v1/`（当前项目域名）。debug 构建固定用模拟器地址 `http://10.0.2.2:3000/api/v1/`，不受影响。

### 2. 构建

```bash
cd android
./gradlew assembleRelease
```

产物：`android/app/build/outputs/apk/release/app-release.apk`（已签名，开启混淆）。

### 3. 分发

- **GitHub Release**：仓库主页 → Releases → 上传 APK，写版本说明
- 或放到服务器 nginx 目录供下载（放 `web-admin/dist/` 外，另开 location）

## 版本升级

每次发布前在 `android/app/build.gradle.kts` 递增 `versionCode`（`versionName` 同步）——已安装用户才能收到更新提示。

## 备份提醒

`enurse-release.keystore` + `local.properties` 是发布凭据。建议：
- keystore 离线备份一份（U 盘/私有网盘）
- 口令记在密码管理器

丢失 keystore = 无法对已发布 App 发升级包。
