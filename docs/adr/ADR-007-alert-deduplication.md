# ADR-007: 报警去重与频率控制

状态：Accepted | 日期：Phase 0

## 决策
- fingerprint = hash(project + service + environment + alert_rule + labels)；相同 fingerprint 短时间内合并为同一 Alert，仅更新 last_occurred_at / occurrence_count。
- 去重窗口与频率配置化：首次立即通知，重复通知间隔 repeat_interval_minutes、最大重复次数 max_repeat_count，超限后停止普通通知。
- 状态：ALERTING / ACKNOWLEDGED / RESOLVED。ACK 停止普通重复通知但**不阻止升级**；超过 escalation delay 未恢复仍触发升级。
- 恢复事件将 Alert 置 RESOLVED 并发送恢复通知（含持续时间）。
- Redis key：`alert:fingerprint:{fingerprint}`。

## TDD 用例基线
1. 同 fingerprint 1000 次 webhook → 1 条 Alert。
2. 首次立即通知；间隔内重放不再通知。
3. ACK 后普通通知停止；到达升级时间仍升级。
4. resolve 后发恢复通知且状态 RESOLVED。
