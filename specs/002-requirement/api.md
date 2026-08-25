# API — 需求管理

统一响应：`ApiResponse{code,message,data,requestId}`；错误分类见 AGENTS.md §二十一（VALIDATION/BUSINESS/AUTH/PERMISSION_DENIED/EXTERNAL/TIMEOUT/CONFLICT/IDEMPOTENCY/SYSTEM）。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/requirements` | 手动创建 |
| GET | `/api/requirements?projectId=` | 列表 |
| POST | `/api/requirements/import` | 外源导入(幂等) |
