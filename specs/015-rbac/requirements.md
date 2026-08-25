# Requirements — IAM 与 RBAC

## 用户故事

- 作为安全管理员，我通过角色-权限矩阵控制谁能执行生产发布/生产确认等高危操作。

## 业务规则

- RBAC 全覆盖生产操作(§二十五)；密码 BCrypt；JWT HS512 签名可配 TTL。
- 权限点编码集中管理(PermissionChecker 常量)，禁止散落字符串。

## 前置条件

- 依赖的上游 Phase/Spec 已完成；相关配置资源已就绪。

## 后置条件

- 本规格范围内数据落库一致，领域事件/审计/日志按规范产出。

## 异常情况

- 凭证错误 → AUTH_ERROR
- 权限不足 → PERMISSION_DENIED

## 权限

- `iam:admin 用户/角色管理`

## 验收标准（摘要）

- 越权访问全部 403
