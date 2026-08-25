# Requirements — 发布计划

## 用户故事

- 作为发布负责人，我创建发布计划并勾选需求/服务/分支/目标环境，驱动后续测试→验收→RC→预发→生产流程。
- 作为审计员，计划开始时的全部相关配置被快照固化，后续配置修改不影响本次发布。

## 业务规则

- 状态机转换矩阵见 ADR-003：非法转换抛 IllegalStateTransitionException。
- READY 后禁止再编辑服务/分支清单(需回退 DRAFT)。
- 进入 TEST_MERGING 前生成 ReleaseConfigSnapshot(config_version 版本递增)。

## 状态转换

DRAFT→READY→TEST_MERGING→…→COMPLETED；FAILED/TIMEOUT/CANCELLED 终态(ADR-003)

## 前置条件

- 依赖的上游 Phase/Spec 已完成；相关配置资源已就绪。

## 后置条件

- 本规格范围内数据落库一致，领域事件/审计/日志按规范产出。

## 异常情况

- 非法状态转换 → CONFLICT
- READY 后改明细 → CONFLICT

## 权限

- `release-plan:write`
- `release-plan:read`
- `release-plan:start 启动发布`

## 验收标准（摘要）

- 非法转换 100% 被拒
- 快照内容与 config_version 一致
