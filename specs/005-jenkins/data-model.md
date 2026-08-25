# Data Model — Jenkins 集成

Flyway 版本化迁移，禁止手改库；所有表含 created_at/updated_at，必要时 version/deleted_at；关键唯一约束数据库级保证。

| 表 | 说明 |
|---|---|
| `jenkins_server` | 地址/凭证(加密) |
| `jenkins_job` | job 名与服务×环境映射 |
| `jenkins_parameter_mapping` | 平台字段→Jenkins 参数 |
