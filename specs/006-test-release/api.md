# API — 测试环境发布

统一响应：`ApiResponse{code,message,data,requestId}`；错误分类见 AGENTS.md §二十一（VALIDATION/BUSINESS/AUTH/PERMISSION_DENIED/EXTERNAL/TIMEOUT/CONFLICT/IDEMPOTENCY/SYSTEM）。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/release-plans/{id}/start` | 发起测试发布 |
| POST | `/api/release-plans/{id}/resolve-conflict` | 标记冲突已解决并重试 |
