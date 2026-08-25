# Test Plan — Kubernetes 部署验证

TDD 强制：先写失败测试（RED→GREEN→REFACTOR）。外部系统用 Fake/WireMock/Testcontainers，禁止真实环境。

## Unit

- Unit(先写)：4/4 Ready=SUCCESS;3/4=RUNNING;unavailable>0=FAILED;CrashLoopBackOff=FAILED;Timeout=TIMEOUT;Version不一致=VERSION_CHECK_FAILED

## Integration / Contract / E2E

- Integration：FakeKubernetesGateway→Adapter→Node 落库

## DoD 关联

- 见 AGENTS.md §三十一 清单，逐项勾选后方可标记完成。
