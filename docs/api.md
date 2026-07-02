# API 设计

## 基础约定

- Base URL: `/api/v1`
- 认证: `Authorization: Bearer <accessToken>`
- 分页: `?page=1&pageSize=20` (max=100)
- 时间: ISO 8601
- 错误: `{ "code": "...", "message": "...", "details": {} }`

## Auth 模块

| Method | Endpoint | 说明 | Auth |
|---|---|---|---|
| POST | `/auth/register` | 邮箱注册 | Public |
| POST | `/auth/login` | 登录 | Public |
| POST | `/auth/refresh` | 刷新 token | Refresh |
| POST | `/auth/logout` | 登出 | JWT |

## Bm 模块

| Method | Endpoint | 说明 |
|---|---|---|
| POST | `/bm/records` | 创建大便记录 |
| GET | `/bm/records` | 查询记录 `?from=&to=` |
| GET | `/bm/records/:id` | 单条详情 |
| PUT | `/bm/records/:id` | 修改备注 |
| DELETE | `/bm/records/:id` | 删除 |
| GET | `/bm/calendar?year=&month=` | 日历视图 |
| GET | `/bm/settings` | 获取提醒设置 |
| PUT | `/bm/settings` | 更新提醒设置 |

## Medications 模块

| Method | Endpoint | 说明 |
|---|---|---|
| POST | `/medications` | 添加药品 |
| GET | `/medications` | 药品列表 |
| PUT | `/medications/:id` | 编辑药品 |
| DELETE | `/medications/:id` | 软删除 |
| POST | `/medications/:id/schedules` | 添加排班 |
| PUT | `/medications/schedules/:id` | 编辑排班 |
| DELETE | `/medications/schedules/:id` | 删除排班 |
| POST | `/medications/records` | 用药打卡 |
| GET | `/medications/records` | 查询记录 |

## Groups 模块

| Method | Endpoint | 说明 |
|---|---|---|
| POST | `/groups` | 创建小组 (NURSE) |
| GET | `/groups` | 我的小组列表 |
| GET | `/groups/:id` | 小组详情 |
| PUT | `/groups/:id` | 修改 (OWNER) |
| DELETE | `/groups/:id` | 删除 (OWNER) |
| POST | `/groups/:id/regenerate-invite` | 重新生成邀请码 |
| POST | `/groups/join` | 通过邀请码加入 |

## 护理端成员接口

| Method | Endpoint | 说明 |
|---|---|---|
| GET | `/groups/:id/members` | 成员列表 |
| DELETE | `/groups/:id/members/:userId` | 移除成员 |
| GET | `/groups/:id/members/:userId/alerts` | 成员警报 |
| GET | `/groups/:id/members/:userId/events` | 成员事件流 |
| GET | `/groups/:id/members/:userId/strategies` | 成员策略 |
| PUT | `/groups/:id/members/:userId/strategies` | 编辑策略 |
| PUT | `/groups/:id/members/:userId/strategies/lock` | 锁定/解锁 |

## Alerts 模块

| Method | Endpoint | 说明 |
|---|---|---|
| GET | `/alerts?filter=active` | 警报列表 |
| PUT | `/alerts/:id/resolve` | 解除警报 |

## Admin 模块

| Method | Endpoint | 说明 |
|---|---|---|
| POST | `/admin/login` | 管理员登录 |
| GET | `/admin/users` | 用户列表 |
| PUT | `/admin/users/:id/role` | 修改角色 |
| DELETE | `/admin/users/:id` | 禁用用户 |
| GET | `/admin/groups` | 所有小组 |
| GET | `/admin/stats` | 系统统计 |
