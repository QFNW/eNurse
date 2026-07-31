# 163 邮箱 SMTP 配置指南

## 第一步：开启 SMTP 服务

1. 登录 [mail.163.com](https://mail.163.com)
2. 点击右上角**设置**（齿轮图标）→ **POP3/SMTP/IMAP**
3. 勾选 **IMAP/SMTP 服务** 或 **POP3/SMTP 服务**，点击**开启**
4. 按提示进行短信验证
5. 验证通过后会生成一个**授权码**，保存好

## 第二步：获取授权码

> 授权码不是登录密码，163 会为第三方邮件客户端生成一个独立密码。

1. 在同一设置页面，点击**新增授权码**
2. 通过短信验证
3. 复制生成的 16 位授权码

## 第三步：配置 .env

编辑 `server/.env`，找到 SMTP 相关配置，修改为：

```
SMTP_HOST=smtp.163.com
SMTP_PORT=465
SMTP_USER=你的邮箱@163.com
SMTP_PASS=你的16位授权码
```

示例：

```
SMTP_HOST=smtp.163.com
SMTP_PORT=465
SMTP_USER=enurse_test@163.com
SMTP_PASS=ABCDEFGHIJKLMNOP
```

## 第四步：重启服务端

双击运行项目根目录的 `start-server.bat`。

启动日志中如果没有打印 `SMTP not configured` 警告，就说明 SMTP 已连通。

## 第五步：测试

1. 打开 App，进入注册页面
2. 输入邮箱地址，点击**获取验证码**
3. 检查收件箱，应收到一封来自「易护 eNurse」的邮件，内含 6 位验证码

## 常见问题

| 问题 | 解决方法 |
|------|---------|
| 启动时出现 `SMTP not configured` | `.env` 里 SMTP_USER 或 SMTP_PASS 为空 |
| `535 Error: authentication failed` | 授权码错误，去 163 设置页重新生成 |
| 连接超时 | 检查防火墙或 VPN；可尝试改端口为 25 |
| 收不到验证码 | 检查垃圾邮箱；确认收件地址拼写正确 |
