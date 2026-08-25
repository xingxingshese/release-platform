# Requirements — Release Branch

## 用户故事

- 作为发布负责人，我需要从 release_test 切出 RC 分支用于预发/生产。

## 业务规则

- 分支名模板来自 Git Branch Rule 配置，默认 release_{yyyyMMdd}_{planId}，禁止硬编码(§三十一)。
- 重复创建幂等：已存在同名分支直接复用(ADR-010)。

## 前置条件

- 依赖的上游 Phase/Spec 已完成；相关配置资源已就绪。

## 后置条件

- 本规格范围内数据落库一致，领域事件/审计/日志按规范产出。

## 异常情况

- 源分支不存在 → NOT_FOUND
- 非 TEST_ACCEPTED 创建 → CONFLICT

## 权限

- `release:branch:create`

## 验收标准（摘要）

- 分支名符合配置模板
