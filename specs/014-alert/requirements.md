# Requirements — 统一报警

## 用户故事

- 作为值班人，我收到首次通知后 ACK 即止住普通重复通知，但超时未恢复仍会升级；恢复时收到恢复消息。

## 业务规则

- fingerprint 去重：同指纹窗口内重复告警合并计数(ADR-007)；1000 次重放→1 Alert。
- ACK≠解决：acknowledged 后超过 escalation_delay 未恢复继续升级(EscalationDecider)。
- Resolved 触发恢复通知；升级时间/级别阈值配置化。

## 前置条件

- 依赖的上游 Phase/Spec 已完成；相关配置资源已就绪。

## 后置条件

- 本规格范围内数据落库一致，领域事件/审计/日志按规范产出。

## 异常情况

- payload 非法 → VALIDATION_ERROR
- 未知指纹字段 → 按 title+labels 兜底生成

## 权限

- `alert:ack 处理告警`
- `alert:admin 规则管理`

## 验收标准（摘要）

- 重放幂等
- 升级/恢复时序正确
