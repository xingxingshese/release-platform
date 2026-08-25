# Requirements — 生产发布与确认

## 用户故事

- 作为授权人，我在生产部署验证通过后做最终确认，发布才算完成。

## 业务规则

- 生产操作必须有权限控制与审计日志(最高优先级规则 §二-7)。
- WAIT_PROD_CONFIRM 超时策略(提醒/自动升级)由配置决定；确认动作幂等(COMPLETED 再确认直接返回)。

## 前置条件

- 依赖的上游 Phase/Spec 已完成；相关配置资源已就绪。

## 后置条件

- 本规格范围内数据落库一致，领域事件/审计/日志按规范产出。

## 异常情况

- 无 prod 权限 → PERMISSION_DENIED
- 非 WAIT_PROD_CONFIRM 确认 → CONFLICT

## 权限

- `release:prod:execute`
- `release:prod:confirm`

## 验收标准（摘要）

- 无权限用户全链路无法触达生产
