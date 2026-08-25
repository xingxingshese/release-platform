# Test Plan — Jenkins 集成

TDD 强制：先写失败测试（RED→GREEN→REFACTOR）。外部系统用 Fake/WireMock/Testcontainers，禁止真实环境。

## Unit

- Contract：WireMock Jenkins(buildWithParameters/queue/build/console)

## Integration / Contract / E2E

- Integration：Webhook 重放幂等
- Unit：参数映射规则

## DoD 关联

- 见 AGENTS.md §三十一 清单，逐项勾选后方可标记完成。
