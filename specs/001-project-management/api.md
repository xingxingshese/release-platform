# API — 项目管理

统一响应：`ApiResponse{code,message,data,requestId}`；错误分类见 AGENTS.md §二十一（VALIDATION/BUSINESS/AUTH/PERMISSION_DENIED/EXTERNAL/TIMEOUT/CONFLICT/IDEMPOTENCY/SYSTEM）。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/projects` | 创建项目 |
| PUT | `/api/projects/{id}` | 更新 |
| DELETE | `/api/projects/{id}` | 软删 |
| POST | `/api/projects/{id}/members` | 添加成员(角色唯一) |
| GET | `/api/projects/{id}/services` | 服务清单 |
| POST | `/api/projects/{id}/services` | 新增服务 |
