# API — 消息通知

统一响应：`ApiResponse{code,message,data,requestId}`；错误分类见 AGENTS.md §二十一（VALIDATION/BUSINESS/AUTH/PERMISSION_DENIED/EXTERNAL/TIMEOUT/CONFLICT/IDEMPOTENCY/SYSTEM）。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/admin/notification-channels` | 渠道配置(secret 加密) |
| POST | `/api/admin/notification-rules` | 路由规则 |
| GET | `/api/admin/notification-records` | 发送记录 |
