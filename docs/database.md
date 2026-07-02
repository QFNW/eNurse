# 数据库设计

完整 Prisma Schema 见 `server/prisma/schema.prisma`。

## 枚举

```prisma
enum Role { PATIENT, NURSE, ADMIN }
enum GroupMemberRole { OWNER, MEMBER }
enum AlertType { BM_REMINDER, MEDICATION_REMINDER }
enum AlertChannel { WECHAT_PRIVATE, WECHAT_GROUP }
enum ScheduleType { INTERVAL, FIXED_TIME }
```

## 核心表关系

```
User(1) ──< BmRecord
User(1) ── (1) BmReminderSetting
User(1) ──< Medication
User(1) ──< Alert
User(1) ──< GroupMember

Medication(1) ──< MedicationSchedule
Medication(1) ──< MedicationRecord
Group(1) ──< GroupMember
Alert(1) ──< SentAlert
Group ──< GroupAlert
```

## 关键索引

- bm_records: (user_id, recorded_at) — 日历查询
- medication_records: (user_id, taken_at) — 用药记录查询
- alerts: (user_id, is_resolved, created_at) — 主页警报
- groups: invite_code UNIQUE — 邀请码查找

## 应用层约束

- 每个 PATIENT 最多属于一个小组
- 护理账号可创建管理多个小组
- isLocked=true 时策略不可被患者端/护理端编辑
