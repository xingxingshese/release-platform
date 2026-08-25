# Test Plan — 项目初始化与基础设施

TDD 强制：先写失败测试（RED→GREEN→REFACTOR）。外部系统用 Fake/WireMock/Testcontainers，禁止真实环境。

## Unit

- 上下文加载测试(SpringBootTest)

## Integration / Contract / E2E

- mvn verify 全绿
- docker compose up -d 后 actuator/health UP

## DoD 关联

- 见 AGENTS.md §三十一 清单，逐项勾选后方可标记完成。
