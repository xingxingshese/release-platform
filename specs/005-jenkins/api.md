# API — Jenkins 集成

统一响应：`ApiResponse{code,message,data,requestId}`；错误分类见 AGENTS.md §二十一（VALIDATION/BUSINESS/AUTH/PERMISSION_DENIED/EXTERNAL/TIMEOUT/CONFLICT/IDEMPOTENCY/SYSTEM）。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/admin/jenkins-servers` | Server 配置(token 加密) |
| POST | `/api/admin/jenkins-jobs` | Job 配置 |
| POST | `/api/admin/jenkins-parameter-mappings` | 参数映射 |
| POST | `/api/webhooks/jenkins` | 构建回调(幂等) |
| POST | `/api/release-tasks/{id}/retry | /cancel` |  |
