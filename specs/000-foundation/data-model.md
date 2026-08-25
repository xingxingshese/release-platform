# Data Model — 项目初始化与基础设施

Flyway 版本化迁移，禁止手改库；所有表含 created_at/updated_at，必要时 version/deleted_at；关键唯一约束数据库级保证。

| 表 | 说明 |
|---|---|
| `operation_log` | 操作审计日志 |
| `config_version` | 配置版本(ADR-008) |
