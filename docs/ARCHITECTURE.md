# ARCHITECTURE.md — 发布管理与统一报警平台总体架构

> 版本：v1.0（Phase 0）
> 配套：`AGENTS.md`（含原 agent.md 全部内容）、`doc/发布管理与统一报警平台——AI Coding 技术设计与实现规范.md`

## 1. 架构风格

**模块化单体（Modular Monolith）+ 事件驱动 + Provider/Adapter 六边形架构。**

- 单一 Spring Boot 进程，按领域分包，包间只通过 `api`（接口）与 `event`（领域事件）交互。
- 外部系统（Git / Jenkins / K8s / OSS / CDN / 通知渠道 / 需求源）一律通过接口抽象接入。
- 后续可按 `release-service / alert-service / notification-service / deployment-service / config-service` 拆分微服务。

## 2. 系统上下文

```
Vue3 前端 ──> REST API (Spring Boot)
                  │
    ┌─────────────┼──────────────────────────┐
    ▼             ▼                          ▼
 Release 域    Alert 域                    IAM/Audit/Config
 (状态机编排)   (webhook→去重→路由→升级)      (RBAC / 快照 / 审计)
    │             │
    ├── GitProvider(抽象)        ├── AlertProvider(Custom Webhook 第一期)
    ├── JenkinsProvider(抽象)    ├── NotificationProvider(WeCom/Feishu/Email/Internal)
    └── DeploymentAdapter(K8s/OSS/CDN/Static/Server/Custom)

异步：RocketMQ(通知/审计/指标)   Redis(锁/幂等/去重/频率)   MySQL(Flyway)
```

## 3. 后端包结构（按领域拆分）

```
backend/src/main/java/com/company/release/
├── common/{exception,response,security,event,lock,idempotency,util}
├── project/       controller,application,domain,infrastructure,repository
├── requirement/   (+ provider: Yunxiao/Jira/Tapd/Custom)
├── repository/    (git repository 管理)
├── environment/
├── release/       controller,application,domain/{model,state,event,service},infrastructure,repository
├── git/           api(GitProvider),provider(gitlab/github/gitee/codeup/custom),domain,infrastructure
├── jenkins/       api(JenkinsProvider),provider,webhook,infrastructure
├── deployment/    api,adapter(kubernetes/frontend/oss/cdn/server/custom),verifier,health,version
├── notification/  provider,routing,template
├── alert/         controller,application,domain,routing,fingerprint,escalation,infrastructure
├── config/        (配置版本 + ReleaseConfigSnapshot)
├── iam/           (RBAC)
└── audit/
```

依赖方向规则：

1. `controller → application → domain`；domain 不依赖 Spring Web/JPA 注解之外的基础设施。
2. 跨域调用只能走目标域的 `api` 包接口或事件。
3. Jenkins/K8s/Git/通知等外部客户端只存在于各域的 `provider`/`adapter`/`infrastructure` 包。

## 4. 核心机制

### 4.1 发布状态机（ADR-003）

状态与转换矩阵集中定义于 `release/domain/state`（`ReleaseStatus` + `ReleaseStateMachine`），非法转换抛 `IllegalStateTransitionException`。全量状态见规范 §8/§44。

### 4.2 发布成功判定（ADR-006）

`ReleaseSuccessEvaluator`：

```
Jenkins SUCCESS AND Deployment SUCCESS
AND (配置启用时) Health Check SUCCESS AND Version Check SUCCESS
=> ReleaseTask SUCCESS => 才触发 Notification
```

K8s Deployment 成功条件：`desired == updated == ready == available && unavailable == 0`；任一 Pod CrashLoopBackOff/ImagePullBackOff/Pending/NotReady/Failed/超时 ⇒ 整体失败。前端项目走 `FrontendVerifier`（HTTP Health + Version Check），不依赖 K8s Pod。

### 4.3 报警流水线（ADR-007）

```
Webhook → Normalize → Fingerprint(project+service+env+rule+labels) → 去重(Redis)
        → AlertRule 匹配 → Routing(接收人/渠道) → 首次通知
        → 频率控制(重复通知间隔/最大次数) ; ACK 停普通重复但升级继续
        → Escalation(level×delay×receivers) → Resolved 恢复通知
```

### 4.4 配置快照（ADR-008）

所有影响发布的配置带版本（config_version）；发布计划启动时复制生成 `ReleaseConfigSnapshot`，执行期间只读快照。

### 4.5 事务与异步边界

DB 事务提交 → 发领域事件到 RocketMQ → 异步 Worker 调 Jenkins/Git/通知。外部调用禁止包在长事务内；失败由状态机 + 重试恢复。分布式锁 key：`release:lock:{releaseTaskId}` 等（Redisson）。幂等：Jenkins Webhook 用 `server_id+job+build_number`，Alert Webhook 用 `project+fingerprint+external_event_id`。

## 5. 数据模型总览

按规范 §二十一 全量表清单建表（project/project_member、requirement、release_plan 及关联表、environment/environment_release_rule、release_flow(+step)、jenkins_server/job/parameter_mapping、deployment_target/config、health_check_config、version_check_config、release_task/release_deployment/release_deployment_node、notification_channel/rule、alert/alert_event/alert_rule/alert_route/alert_escalation、role/permission/user_role、config_version/release_config_snapshot、operation_log）。所有表含主键、索引、created_at/updated_at/version（乐观锁），敏感凭证加密存储。

详细字段见各 `specs/0xx-*/data-model.md` 与 `docs/database.md`。

## 6. API 规范

REST + OpenAPI（springdoc）。统一响应 `{code,message,data,requestId}`；错误模型 `{code,message,requestId,details}`，错误码枚举：VALIDATION_ERROR / BUSINESS_ERROR / AUTH_ERROR / PERMISSION_DENIED / EXTERNAL_SERVICE_ERROR / TIMEOUT / CONFLICT / IDEMPOTENCY_ERROR / SYSTEM_ERROR。API 清单见规范 §59/§60。

## 7. 可观测性

Micrometer + Prometheus 指标：release_total、release_success_total、release_failed_total、release_duration、jenkins_build_duration、deployment_duration、k8s_deployment_success_total、alert_total、alert_escalation_total、notification_success_total、notification_failed_total。结构化日志含 requestId/traceId/userId/projectId/releasePlanId/releaseTaskId/serviceId，敏感字段脱敏。

## 8. 前端架构

Vue 3 + TS Strict + Pinia + Vue Router + Element Plus；目录按 AGENTS.md §十一（app/layouts/pages/features/components/api/hooks/types/utils/styles）。发布详情用 Timeline 组件逐层展开（Jenkins/Deployment/Pod/Health/Version/日志）；数据刷新先 Polling（5s）后升级 SSE。

## 9. 关键 ADR 索引

见 `docs/adr/`：monorepo、模块化单体、发布状态机、Jenkins Adapter、Deployment Adapter、K8s 成功策略、报警去重、配置快照、事件驱动通知、幂等。
