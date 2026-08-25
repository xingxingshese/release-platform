# API — 生产发布与确认

统一响应：`ApiResponse{code,message,data,requestId}`；错误分类见 AGENTS.md §二十一（VALIDATION/BUSINESS/AUTH/PERMISSION_DENIED/EXTERNAL/TIMEOUT/CONFLICT/IDEMPOTENCY/SYSTEM）。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/release-plans/{id}/deploy-prod` | 发起生产(鉴权) |
| POST | `/api/release-plans/{id}/confirm` | 生产确认(幂等) |
