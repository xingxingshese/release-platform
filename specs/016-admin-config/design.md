# Design — 管理员配置中心与配置版本

AdminConfigService.saveVersion 事务内 SELECT MAX(version)+1；content JSON 存储便于 diff；快照复制发生在发布启动事务(003)。

## 设计约束（全局）

- 模块化单体，跨域仅经 `api` 接口与领域事件（ADR-002）。
- 外部系统一律 Provider/Adapter；测试用 Fake/WireMock（规范 §十九/二十）。
- 事务边界：外部调用不得包在长事务内（规范 §二十二）；发布类操作加分布式锁、Webhook/重试幂等（ADR-010）。
- 日志含 requestId/traceId/userId/业务 ID；敏感字段脱敏（规范 §二十六）。
