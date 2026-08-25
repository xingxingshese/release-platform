# Test Plan — 测试环境发布

TDD 强制：先写失败测试（RED→GREEN→REFACTOR）。外部系统用 Fake/WireMock/Testcontainers，禁止真实环境。

## Unit

- E2E(Fake)：全链路 READY→WAIT_TEST_ACCEPT

## Integration / Contract / E2E

- 并发：双触发仅一次执行
- 超时转 TIMEOUT 用例

## DoD 关联

- 见 AGENTS.md §三十一 清单，逐项勾选后方可标记完成。
