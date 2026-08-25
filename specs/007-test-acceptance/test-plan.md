# Test Plan — 测试验收

TDD 强制：先写失败测试（RED→GREEN→REFACTOR）。外部系统用 Fake/WireMock/Testcontainers，禁止真实环境。

## Unit

- Unit：状态守卫+幂等

## Integration / Contract / E2E

- 权限：无权限点拒绝

## DoD 关联

- 见 AGENTS.md §三十一 清单，逐项勾选后方可标记完成。
