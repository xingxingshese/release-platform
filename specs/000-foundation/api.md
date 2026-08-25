# API — 项目初始化与基础设施

统一响应：`ApiResponse{code,message,data,requestId}`；错误分类见 AGENTS.md §二十一（VALIDATION/BUSINESS/AUTH/PERMISSION_DENIED/EXTERNAL/TIMEOUT/CONFLICT/IDEMPOTENCY/SYSTEM）。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/actuator/health` | 健康检查(UP 才算环境就绪) |
| GET | `/v3/api-docs` | OpenAPI 文档 |
