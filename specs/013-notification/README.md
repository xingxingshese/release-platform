# 013-notification · 消息通知

> NotificationChannel/Rule 配置化路由(事件×对象×渠道×接收人)、Provider(WeCom/Feishu/Email/Internal)、异步发送(MQ/Spring Event)、发送失败不影响发布与报警主流程(ADR-009)。

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
