# Design — 项目初始化与基础设施

模块化单体 + 领域分包(common/project/release/git/jenkins/deployment/notification/alert/config/iam/audit)；统一 ApiResponse{code,message,data,requestId}；全局异常处理映射 ErrorCode。

## 设计约束（全局）

- 模块化单体，跨域仅经 `api` 接口与领域事件（ADR-002）。
- 外部系统一律 Provider/Adapter；测试用 Fake/WireMock（规范 §十九/二十）。
- 事务边界：外部调用不得包在长事务内（规范 §二十二）；发布类操作加分布式锁、Webhook/重试幂等（ADR-010）。
- 日志含 requestId/traceId/userId/业务 ID；敏感字段脱敏（规范 §二十六）。
