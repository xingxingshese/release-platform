# Data Model — 发布计划

Flyway 版本化迁移，禁止手改库；所有表含 created_at/updated_at，必要时 version/deleted_at；关键唯一约束数据库级保证。

| 表 | 说明 |
|---|---|
| `release_plan` | 主表(status/environments/config_snapshot_id) |
| `release_plan_requirement` | 计划×需求 |
| `release_plan_member` | 计划成员 |
| `release_plan_service` | 计划×服务×分支(release_branch 回填) |
| `release_task` | 环境 task(TEST/PRE/PROD) |
| `release_config_snapshot` | 发布级配置快照 |
