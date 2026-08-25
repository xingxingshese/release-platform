# Requirements — 前端项目部署

## 用户故事

- 作为前端负责人，前端项目发布后平台通过 HTTP 探活与 version.json 比对判定成败。

## 业务规则

- 前端发布不强依赖 Kubernetes(§34)；expectedVersion 来自构建产物登记。
- health 非 200 或 version 不一致 → 判败并阻断'部署成功'通知。

## 前置条件

- 依赖的上游 Phase/Spec 已完成；相关配置资源已就绪。

## 后置条件

- 本规格范围内数据落库一致，领域事件/审计/日志按规范产出。

## 异常情况

- 探活超时 → health failed
- version.json 缺失 → version failed

## 权限

- `deployment:read`

## 验收标准（摘要）

- 三分支用例覆盖
- 与后端共用统一 VerifyOutcome
