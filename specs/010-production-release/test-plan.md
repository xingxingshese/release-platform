# Test Plan — 生产发布与确认

TDD 强制：先写失败测试（RED→GREEN→REFACTOR）。外部系统用 Fake/WireMock/Testcontainers，禁止真实环境。

## Unit

- Unit：权限拒绝/确认幂等/超时策略

## Integration / Contract / E2E

- Audit：确认动作留痕

## DoD 关联

- 见 AGENTS.md §三十一 清单，逐项勾选后方可标记完成。
