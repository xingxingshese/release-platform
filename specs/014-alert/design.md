# Design — 统一报警

AlertIngestService 归一化+去重；AlertEscalationJob 定时扫描升级；通知经 NotificationDispatcher(ADR-009)。

## 设计约束（全局）

- 模块化单体，跨域仅经 `api` 接口与领域事件（ADR-002）。
- 外部系统一律 Provider/Adapter；测试用 Fake/WireMock（规范 §十九/二十）。
- 事务边界：外部调用不得包在长事务内（规范 §二十二）；发布类操作加分布式锁、Webhook/重试幂等（ADR-010）。
- 日志含 requestId/traceId/userId/业务 ID；敏感字段脱敏（规范 §二十六）。
