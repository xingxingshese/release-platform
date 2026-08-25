# API — 发布计划

统一响应：`ApiResponse{code,message,data,requestId}`；错误分类见 AGENTS.md §二十一（VALIDATION/BUSINESS/AUTH/PERMISSION_DENIED/EXTERNAL/TIMEOUT/CONFLICT/IDEMPOTENCY/SYSTEM）。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/release-plans` | 创建(DRAFT) |
| POST | `/api/release-plans/{id}/ready` | 提交就绪 |
| POST | `/api/release-plans/{id}/requirements` | 关联需求 |
| POST | `/api/release-plans/{id}/services` | 关联服务分支 |
| POST | `/api/release-plans/{id}/start` | 启动测试发布 |
