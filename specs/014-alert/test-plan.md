# Test Plan — 统一报警

TDD 强制：先写失败测试（RED→GREEN→REFACTOR）。外部系统用 Fake/WireMock/Testcontainers，禁止真实环境。

## Unit

- Unit：fingerprint 稳定性、NotifyDecider、EscalationDecider 矩阵

## Integration / Contract / E2E

- Integration：1000 重放→1 条；ACK 后升级仍触发；恢复通知内容
- E2E：webhook→通知→ACK→升级→resolve

## DoD 关联

- 见 AGENTS.md §三十一 清单，逐项勾选后方可标记完成。
