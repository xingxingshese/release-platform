# Requirements — Jenkins 集成

## 用户故事

- 作为管理员，我配置 Jenkins Server/Job 与参数映射，使发布计划无需硬编码即可触发构建。

## 业务规则

- Jenkins 一律经 JenkinsProvider Adapter，禁止 ReleaseService 直调 HTTP(规范 §四/§30-6)。
- Webhook 以 build 号+job 幂等(ADR-010)；轮询兜底防止 webhook 丢失；SUCCESS≠部署成功(§五)。
- Jenkins Token 加密存储。

## 前置条件

- 依赖的上游 Phase/Spec 已完成；相关配置资源已就绪。

## 后置条件

- 本规格范围内数据落库一致，领域事件/审计/日志按规范产出。

## 异常情况

- queue 未出 build → 轮询退避后 TIMEOUT
- webhook 重放 → 幂等忽略

## 权限

- `jenkins:admin 配置`
- `jenkins:retry 重试`
- `jenkins:cancel 取消`

## 验收标准（摘要）

- 重放 100 次 webhook 仅生效一次
- Token 密文断言
