# API — 统一报警

统一响应：`ApiResponse{code,message,data,requestId}`；错误分类见 AGENTS.md §二十一（VALIDATION/BUSINESS/AUTH/PERMISSION_DENIED/EXTERNAL/TIMEOUT/CONFLICT/IDEMPOTENCY/SYSTEM）。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/webhooks/alerts` | 报警接入(Custom Webhook) |
| POST | `/api/alerts/{id}/ack` | 确认 |
| POST | `/api/alerts/{id}/resolve` | 恢复 |
