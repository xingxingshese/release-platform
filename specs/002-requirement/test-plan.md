# Test Plan — 需求管理

TDD 强制：先写失败测试（RED→GREEN→REFACTOR）。外部系统用 Fake/WireMock/Testcontainers，禁止真实环境。

## Unit

- Contract：FakeRequirementServer 合同测试

## Integration / Contract / E2E

- Unit：导入幂等(同 key 二次导入不新增)
- Integration：需求↔发布计划关联

## DoD 关联

- 见 AGENTS.md §三十一 清单，逐项勾选后方可标记完成。
