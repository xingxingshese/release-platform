# Test Plan — 发布计划

TDD 强制：先写失败测试（RED→GREEN→REFACTOR）。外部系统用 Fake/WireMock/Testcontainers，禁止真实环境。

## Unit

- Unit：状态机全矩阵(合法+非法)

## Integration / Contract / E2E

- Integration：快照版本化、READY 锁定明细
- E2E(Fake)：DRAFT→…→WAIT_TEST_ACCEPT

## DoD 关联

- 见 AGENTS.md §三十一 清单，逐项勾选后方可标记完成。
