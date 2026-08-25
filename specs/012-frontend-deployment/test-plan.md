# Test Plan — 前端项目部署

TDD 强制：先写失败测试（RED→GREEN→REFACTOR）。外部系统用 Fake/WireMock/Testcontainers，禁止真实环境。

## Unit

- Unit：注入 Fake httpFetcher/bodyFetcher 三分支(健康失败/版本失败/通过)

## Integration / Contract / E2E

- Integration：FakeOSS 上传后端到端验证

## DoD 关联

- 见 AGENTS.md §三十一 清单，逐项勾选后方可标记完成。
