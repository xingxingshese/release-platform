# Requirements — 测试环境发布

## 用户故事

- 作为发布负责人，我一键发起测试发布；若 merge 冲突则计划挂起等待人工解决后重试。

## 业务规则

- 分布式锁 release:lock:{planId} 防并发重复触发(§二十三)；冲突禁止自动绕过(§三)。
- 构建结束必须 Deployment+Health+Version 复核后才 SUCCESS(§五)。

## 前置条件

- 依赖的上游 Phase/Spec 已完成；相关配置资源已就绪。

## 后置条件

- 本规格范围内数据落库一致，领域事件/审计/日志按规范产出。

## 异常情况

- 非 READY 发起 → CONFLICT
- 锁占用 → CONFLICT '任务运行中'
- 超时 → TIMEOOUT 状态

## 权限

- `release:test:execute`

## 验收标准（摘要）

- 重复触发被拒绝且无副作用
