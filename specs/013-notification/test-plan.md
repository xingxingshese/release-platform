# Test Plan — 消息通知

TDD 强制：先写失败测试（RED→GREEN→REFACTOR）。外部系统用 Fake/WireMock/Testcontainers，禁止真实环境。

## Unit

- Unit：路由匹配(事件/项目/环境/级别维度)

## Integration / Contract / E2E

- Integration：FakeSender 全链路；失败渠道不影响其余渠道
- 回归：主流程异常注入仍成功

## DoD 关联

- 见 AGENTS.md §三十一 清单，逐项勾选后方可标记完成。
