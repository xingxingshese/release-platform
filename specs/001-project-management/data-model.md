# Data Model — 项目管理

Flyway 版本化迁移，禁止手改库；所有表含 created_at/updated_at，必要时 version/deleted_at；关键唯一约束数据库级保证。

| 表 | 说明 |
|---|---|
| `project` | 项目主表(project_type/environments 等) |
| `project_member` | 项目成员与六类负责人 |
| `service` | 项目服务及关联配置 |
