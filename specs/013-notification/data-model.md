# Data Model — 消息通知

Flyway 版本化迁移，禁止手改库；所有表含 created_at/updated_at，必要时 version/deleted_at；关键唯一约束数据库级保证。

| 表 | 说明 |
|---|---|
| `notification_channel(code/type/config/enabled)` |  |
| `notification_rule(event_type/project/environment/min_level/channel_code/receiver_template)` |  |
| `notification_record(发送结果)` |  |
