# Test Plan — 预发发布

TDD 强制：先写失败测试（RED→GREEN→REFACTOR）。外部系统用 Fake/WireMock/Testcontainers，禁止真实环境。

## Unit

- 编排：并行开/关两种模式守卫用例

## Integration / Contract / E2E

- 锁防重复

## DoD 关联

- 见 AGENTS.md §三十一 清单，逐项勾选后方可标记完成。
