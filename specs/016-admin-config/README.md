# 016-admin-config · 管理员配置中心与配置版本

> 管理员配置资源(环境/发布规则/Jenkins/Git/部署/健康检查/通知/报警/RBAC/系统参数)统一管理；config_version 版本递增与 diff 对比；发布快照不受后续修改影响(ADR-008)。

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
