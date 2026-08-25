# Requirements — 需求管理

## 用户故事

- 作为产品/开发，我可在平台录入需求并与发布计划关联。
- 作为集成方，我可通过 Provider 接口从云效等外部需求源导入需求(幂等)。

## 业务规则

- 外部需求源一律走 RequirementProvider Adapter，禁止在业务层直调外部 API(规范 §2)。
- 导入以外源 external_key 幂等：重复导入更新而非新增。

## 前置条件

- 依赖的上游 Phase/Spec 已完成；相关配置资源已就绪。

## 后置条件

- 本规格范围内数据落库一致，领域事件/审计/日志按规范产出。

## 异常情况

- 外源不可达 → EXTERNAL_SERVICE_ERROR 且不影响平台功能
- external_key 重复 → 更新已有记录

## 权限

- `requirement:write`
- `requirement:read`

## 验收标准（摘要）

- 导入重放 100 次 → 记录数不变
