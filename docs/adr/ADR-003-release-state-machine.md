# ADR-003: 发布状态集中状态机管理

状态：Accepted | 日期：Phase 0

## 决策
`ReleaseStatus` 枚举 + `ReleaseStateMachine` 集中维护全部合法转换（DRAFT→…→COMPLETED 及 FAILED/TIMEOUT/CANCELLED），非法转换抛 `IllegalStateTransitionException` 并记录审计。禁止业务代码散落 if/else 改状态。

## 关键规则
- 冲突只能进入 WAIT_CONFLICT_RESOLVE，禁止自动绕过。
- 仅 TEST_ACCEPTED 可创建 Release Branch；仅 RELEASE_BRANCH_CREATED/PRE_DEPLOY_SUCCESS 可进入 PROD（并行由配置决定）。
- 仅 WAIT_PROD_CONFIRM 且确认通过才 COMPLETED。
- 每次转换持久化事件（ReleaseStatusChanged）供 Timeline 与通知使用。

## 测试要求
全量转换矩阵单测覆盖（合法/非法各一）。
