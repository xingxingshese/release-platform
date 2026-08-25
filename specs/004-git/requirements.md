# Requirements — Git 仓库与分支

## 用户故事

- 作为发布负责人，我发起 merge release_test 时平台返回冲突文件清单以便定位处理。

## 业务规则

- Git Token 加密存储(规范 §25)；日志脱敏不打印 token。
- 冲突绝不自动绕过(WAIT_CONFLICT_RESOLVE)；provider_type 由 repository 配置决定，禁止硬编码(§八)。

## 前置条件

- 依赖的上游 Phase/Spec 已完成；相关配置资源已就绪。

## 后置条件

- 本规格范围内数据落库一致，领域事件/审计/日志按规范产出。

## 异常情况

- 认证失败 → EXTERNAL_SERVICE_ERROR
- 合并冲突 → 返回 conflictFiles 列表

## 权限

- `git:write`
- `git:read`

## 验收标准（摘要）

- 冲突场景返回完整文件列表
- 凭证密文存储验证
