# API — 前端项目部署

统一响应：`ApiResponse{code,message,data,requestId}`；错误分类见 AGENTS.md §二十一（VALIDATION/BUSINESS/AUTH/PERMISSION_DENIED/EXTERNAL/TIMEOUT/CONFLICT/IDEMPOTENCY/SYSTEM）。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/deployments/frontend/verify` | 触发前端验证(内部) |
| GET | `/api/release-tasks/{id}/nodes` | 节点明细 |
