# Test Plan — IAM 与 RBAC

TDD 强制：先写失败测试（RED→GREEN→REFACTOR）。外部系统用 Fake/WireMock/Testcontainers，禁止真实环境。

## Unit

- Unit：JwtService 过期/篡改；PermissionChecker 矩阵

## Integration / Contract / E2E

- Integration：登录→携带 JWT 访问受保护接口
- 负路径：无 token/伪 token 401/403

## DoD 关联

- 见 AGENTS.md §三十一 清单，逐项勾选后方可标记完成。
