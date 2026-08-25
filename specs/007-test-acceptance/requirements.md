# Requirements — 测试验收

## 用户故事

- 作为测试负责人，我在测试通过后验收，或驳回并附原因。

## 业务规则

- 仅 WAIT_TEST_ACCEPT 可决策；重复同一决定幂等返回(ADR-010)。
- reject 必须附 reason；TEST_REJECTED 可修复后重新发起测试发布。

## 前置条件

- 依赖的上游 Phase/Spec 已完成；相关配置资源已就绪。

## 后置条件

- 本规格范围内数据落库一致，领域事件/审计/日志按规范产出。

## 异常情况

- 非 WAIT_TEST_ACCEPT 决策 → CONFLICT

## 权限

- `release:test-accept`

## 验收标准（摘要）

- 重复 accept 不改变状态
