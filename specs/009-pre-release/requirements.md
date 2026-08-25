# Requirements — 预发发布

## 用户故事

- 作为发布负责人，我可将 RC 分支发布到预发环境验证。

## 业务规则

- prodStartAllowed 守卫：并行关闭时仅 PRE_DEPLOY_SUCCESS 可发 PROD；并行开启时 RELEASE_BRANCH_CREATED 亦可(§三)。
- PRE 使用 release_branch 分支构建；PRE 成功判定同样走四条件红线。

## 前置条件

- 依赖的上游 Phase/Spec 已完成；相关配置资源已就绪。

## 后置条件

- 本规格范围内数据落库一致，领域事件/审计/日志按规范产出。

## 异常情况

- 前置状态不满足 → CONFLICT

## 权限

- `release:pre:execute`

## 验收标准（摘要）

- 两种模式行为符合 ADR-003 矩阵
