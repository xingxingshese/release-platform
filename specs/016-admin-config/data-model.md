# Data Model — 管理员配置中心与配置版本

Flyway 版本化迁移，禁止手改库；所有表含 created_at/updated_at，必要时 version/deleted_at；关键唯一约束数据库级保证。

| 表 | 说明 |
|---|---|
| `config_version(config_type/config_key/version/content JSON/changed_by/change_reason)` |  |
