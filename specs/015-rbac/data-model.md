# Data Model — IAM 与 RBAC

Flyway 版本化迁移，禁止手改库；所有表含 created_at/updated_at，必要时 version/deleted_at；关键唯一约束数据库级保证。

| 表 | 说明 |
|---|---|
| `user(username/password_hash/enabled)` |  |
| `role(code)` |  |
| `permission(code)` |  |
| `user_role` |  |
| `role_permission` |  |
