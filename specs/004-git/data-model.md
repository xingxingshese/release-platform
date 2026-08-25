# Data Model — Git 仓库与分支

Flyway 版本化迁移，禁止手改库；所有表含 created_at/updated_at，必要时 version/deleted_at；关键唯一约束数据库级保证。

| 表 | 说明 |
|---|---|
| `repository` | 仓库(provider_type/url 默认分支) |
| `repository_credential` | 加密凭证 |
