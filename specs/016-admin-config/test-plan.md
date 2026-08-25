# Test Plan — 管理员配置中心与配置版本

TDD 强制：先写失败测试（RED→GREEN→REFACTOR）。外部系统用 Fake/WireMock/Testcontainers，禁止真实环境。

## Unit

- Unit：版本递增、diff 字段级算法

## Integration / Contract / E2E

- Integration：保存后旧快照读取不变(回归)
- 乐观锁并发用例

## DoD 关联

- 见 AGENTS.md §三十一 清单，逐项勾选后方可标记完成。
