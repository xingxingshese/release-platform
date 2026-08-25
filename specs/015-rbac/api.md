# API — IAM 与 RBAC

统一响应：`ApiResponse{code,message,data,requestId}`；错误分类见 AGENTS.md §二十一（VALIDATION/BUSINESS/AUTH/PERMISSION_DENIED/EXTERNAL/TIMEOUT/CONFLICT/IDEMPOTENCY/SYSTEM）。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/login` | 登录签发 JWT |
| GET | `/api/auth/me` | 当前用户与权限 |
| POST | `/api/admin/users | /roles` | 管理接口 |
