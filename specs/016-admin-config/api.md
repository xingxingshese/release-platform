# API — 管理员配置中心与配置版本

统一响应：`ApiResponse{code,message,data,requestId}`；错误分类见 AGENTS.md §二十一（VALIDATION/BUSINESS/AUTH/PERMISSION_DENIED/EXTERNAL/TIMEOUT/CONFLICT/IDEMPOTENCY/SYSTEM）。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/admin/configs/{type}/{key}/versions` | 保存新版本 |
| GET | `/api/admin/configs/{type}/{key}/versions` | 版本历史 |
| GET | `/api/admin/configs/{type}/{key}/diff?v1=&v2=` | 字段级对比 |
