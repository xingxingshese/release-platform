# ADR-010: 幂等策略

状态：Accepted | 日期：Phase 0

## 决策
两类幂等机制：

1. **业务唯一键幂等**
   - Jenkins Webhook：`(jenkins_server_id, job_name, build_number)` 唯一约束，重复回调直接返回原结果。
   - Alert Webhook：`(project_id, fingerprint, external_event_id)`。
2. **Idempotency-Key Header 幂等**
   - Release retry/callback、test-accept、prod-confirm、create-release-branch 等写操作接口支持 `Idempotency-Key`，服务端存储请求指纹+响应，重复请求返回缓存响应。

并发防护配合 Redis 分布式锁：`release:lock:{releaseTaskId}`、`alert:escalation:{alertId}:{level}` 等；已锁则拒绝重复执行（CONFLICT）。

## 测试要求
每个幂等点必须有重放测试（同一请求发 ≥2 次，效果等同 1 次）。
