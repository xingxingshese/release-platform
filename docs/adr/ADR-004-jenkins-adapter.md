# ADR-004: Jenkins 通过 Provider/Adapter 接入

状态：Accepted | 日期：Phase 0

## 决策
定义 `JenkinsProvider` 接口：getJob/build/buildWithParameters/getQueueItem/getBuild/getBuildConsole/stopBuild。实现 `DefaultJenkinsProvider`（REST）+ 测试用 `FakeJenkinsServer`（WireMock/Testcontainers）。ReleaseService 只依赖接口。

参数不硬编码：调用时按 `jenkins_parameter_mapping` 动态组装（sourceBranch→BRANCH 等）。Job 选择由 jenkins_job 配置（service×environment）决定。凭证加密存储，Webhook 主通道 + Polling 兜底。

## 禁止
ReleaseService 中出现 Jenkins HTTP 调用、Job 名、Token 字面量。
