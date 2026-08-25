# Data Model — 统一报警

Flyway 版本化迁移，禁止手改库；所有表含 created_at/updated_at，必要时 version/deleted_at；关键唯一约束数据库级保证。

| 表 | 说明 |
|---|---|
| `alert(fingerprint/status/severity/count/escalation_level/notified_at/acked_by…)` |  |
| `notification_record(通知留痕)` |  |
