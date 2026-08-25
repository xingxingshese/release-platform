# Requirements — 项目初始化与基础设施

## 用户故事

- 作为开发者，我需要一条命令启动本地依赖(MySQL/Redis/RocketMQ)，以便本地开发调试。
- 作为开发者，我需要 Flyway 管理全部表结构变更，禁止手改库。

## 业务规则

- 数据库迁移必须版本化(V1..Vn)且只增不改；公共表 operation_log/config_version 在 V1 建立。
- 后端 Java 17 + Spring Boot 3；前端 Vue3 + TS Strict；构建命令以 AGENTS.md §二十四 为准。
- 所有日志行携带 requestId/traceId MDC 字段。

## 前置条件

- 依赖的上游 Phase/Spec 已完成；相关配置资源已就绪。

## 后置条件

- 本规格范围内数据落库一致，领域事件/审计/日志按规范产出。

## 异常情况

- compose 启动失败时 fail-fast 并提示端口占用。

## 权限

- `无业务权限；DB 凭证仅经环境变量注入，禁止提交真实 Secret。`

## 验收标准（摘要）

- mvn verify 绿
- pnpm build 通过
- compose 三件套健康
