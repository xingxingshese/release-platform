# Test Plan — 项目管理

TDD 强制：先写失败测试（RED→GREEN→REFACTOR）。外部系统用 Fake/WireMock/Testcontainers，禁止真实环境。

## Unit

- Unit：成员角色唯一性、软删过滤

## Integration / Contract / E2E

- Integration：CRUD+成员+Service 子资源链路
- 权限边界：无 project:write 返回 PERMISSION_DENIED

## DoD 关联

- 见 AGENTS.md §三十一 清单，逐项勾选后方可标记完成。
