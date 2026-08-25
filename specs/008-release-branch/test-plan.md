# Test Plan — Release Branch

TDD 强制：先写失败测试（RED→GREEN→REFACTOR）。外部系统用 Fake/WireMock/Testcontainers，禁止真实环境。

## Unit

- Unit：模板渲染(日期/planId 注入)

## Integration / Contract / E2E

- Integration：重复创建幂等(FakeGit)

## DoD 关联

- 见 AGENTS.md §三十一 清单，逐项勾选后方可标记完成。
