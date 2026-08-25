# Test Plan — Git 仓库与分支

TDD 强制：先写失败测试（RED→GREEN→REFACTOR）。外部系统用 Fake/WireMock/Testcontainers，禁止真实环境。

## Unit

- Integration：FakeGitServer 含冲突场景

## Integration / Contract / E2E

- Contract：GitLab Provider 合同(WireMock)
- 安全：日志不含 token 断言

## DoD 关联

- 见 AGENTS.md §三十一 清单，逐项勾选后方可标记完成。
