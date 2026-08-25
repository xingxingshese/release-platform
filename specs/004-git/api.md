# API — Git 仓库与分支

统一响应：`ApiResponse{code,message,data,requestId}`；错误分类见 AGENTS.md §二十一（VALIDATION/BUSINESS/AUTH/PERMISSION_DENIED/EXTERNAL/TIMEOUT/CONFLICT/IDEMPOTENCY/SYSTEM）。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/repositories` | 登记仓库(provider_type/url) |
| POST | `/api/repositories/{id}/credential` | 凭证(加密) |
| POST | `/api/git/merge` | 执行 merge(返回 commitId 或冲突清单) |
