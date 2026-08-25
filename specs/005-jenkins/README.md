# 005-jenkins · Jenkins 集成

> JenkinsServer/Job/ParameterMapping 管理、JenkinsProvider 抽象、buildWithParameters→Queue→Build 追踪、Webhook(幂等)+Polling 兜底、Retry/Cancel、凭证加密。

## 文档导航（SDD 七件套）

- [requirements.md](requirements.md) — 用户故事/业务规则/前后置/异常/权限/状态/验收标准
- [design.md](design.md) — 设计要点
- [api.md](api.md) — API Contract
- [data-model.md](data-model.md) — 数据模型
- [test-plan.md](test-plan.md) — 测试计划（分层）
- [acceptance.md](acceptance.md) — 验收清单

## 相关 ADR

ADR-002 modular-monolith · ADR-003 release-state-machine · ADR-004 jenkins-adapter ·
ADR-005 deployment-adapter · ADR-007 alert-deduplication · ADR-008 config-snapshot · ADR-010 idempotency（按相关性取用）
