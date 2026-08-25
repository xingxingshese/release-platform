# IMPLEMENTATION_PLAN.md — 实施计划

> **策略调整（2026-08-25，用户指令）**：先实现后端全部逻辑（保持 TDD），再实现前端逻辑；前端暂不使用 TDD。前端骨架已建，`pnpm install` 因构建脚本审批待处理（`pnpm approve-builds` 或已在 package.json 配置 onlyBuiltDependencies），延后验证。

> 规则：每个 Phase 遵循 Specification → Test Plan → RED → GREEN → REFACTOR → Integration Test → Review → Documentation → DoD。
> 状态：⬜ 未开始 / 🔄 进行中 / ✅ 完成

---

## Phase 0 项目初始化 — ⬜
- **目标**：Monorepo 脚手架、backend/frontend 工程骨架、docker-compose(dev: MySQL/Redis/RocketMQ)、Flyway V1、CI 检查脚本、AGENTS.md。
- **依赖**：无
- **Specification**：specs/000-foundation
- **任务**：T0.1 backend Maven 骨架+空上下文启动测试；T0.2 frontend Vite+Vue3+TS Strict 骨架；T0.3 docker-compose.dev.yml；T0.4 Flyway 初始化与公共表（operation_log/config_version）；T0.5 scripts/ci 基础脚本。
- **测试**：Spring 上下文加载测试、`mvn verify`、`pnpm build` 通过。
- **验收**：`docker compose up -d` 后后端可启动并连上 MySQL/Redis/MQ；健康检查 `/actuator/health` UP。
- **状态**：🔄 后端完成（上下文加载测试绿、Flyway V1、compose、scripts）；前端骨架完成但未验证

### 已完成后端核心逻辑（跨 Phase 提前交付，均 TDD）
- ReleaseStateMachine 全量转换矩阵（ADR-003）— 8 用例 ✅
- KubernetesDeploymentVerifier 成功判定（ADR-006 六用例基线）✅
- ReleaseSuccessEvaluator 最终成功判定（规范 §66）✅
- AlertFingerprintBuilder / AlertNotifyDecider / EscalationDecider（ADR-007）✅

## Phase 1 基础设施（common）— ⬜
统一响应/错误模型/异常处理、requestId TraceId MDC、Redis 分布式锁、幂等组件、审计注解、OpenAPI 配置。测试：错误模型单测、幂等组件集成测试。

## Phase 2 IAM / RBAC — ⬜
用户/角色/权限/JWT 登录/生产发布独立权限点。Specs: specs/015-rbac。测试：权限判定单测 + 登录/鉴权集成测试。

## Phase 3 Project — ⬜
项目 CRUD、project_type、Service 子资源、项目成员六类负责人、项目级权限。Specs: specs/001-project-management。测试：成员角色唯一性/多成员规则、权限边界。

## Phase 4 Requirement — ⬜
手动需求 CRUD + RequirementProvider 抽象 + YunxiaoProvider(Stub 可换真实)。Specs: specs/002-requirement。测试：Provider Mock 合同测试、导入幂等。

## Phase 5 Release Plan — ⬜
发布计划 CRUD、需求关联、多服务分支关联、ReleasePlanMember、状态机（ReleaseStateMachine 全量转换矩阵 TDD）、环境 ReleaseTask 生成、配置快照生成。Specs: specs/003-release-plan。测试：状态机全矩阵单测、快照版本化集成测试。

## Phase 6 Git — ⬜
GitProvider 抽象 + GitLab Provider + FakeGitServer、Repository/Credential 管理、mergeBranch/checkMerge、冲突检测返回冲突文件列表。Specs: specs/004-git。测试：FakeGitServer 集成测试（含冲突场景）。

## Phase 7 Jenkins — ⬜
JenkinsServer/Job/ParameterMapping 管理、JenkinsProvider 抽象、buildWithParameters→Queue→Build 追踪、Webhook（幂等）+ Polling 兜底、Retry/Cancel、凭证加密存储。Specs: specs/005-jenkins。测试：WireMock Jenkins 合同测试、Webhook 重放幂等测试。

## Phase 8 Test Release — ⬜
测试发布编排：前置校验→merge release_test→冲突→WAIT_CONFLICT_RESOLVE→Jenkins→DeploymentVerifier。分布式锁防重复触发。Specs: specs/006-test-release。测试：E2E(Fake) 全链路、并发重复触发拒绝、超时状态。

## Phase 9 Test Acceptance — ⬜
WAIT_TEST_ACCEPT → accept/reject，仅 TEST_ACCEPTED 允许后续。Specs: specs/007-test-acceptance。测试：状态守卫单测、验收接口幂等。

## Phase 10 Release Branch — ⬜
release_{yyyyMMdd}_{releasePlanId} 模板配置化创建，保存到 ReleasePlanService.release_branch。Specs: specs/008-release-branch。测试：模板渲染、重复创建幂等。

## Phase 11 PRE — ⬜
预发 ReleaseTask、PRE/PROD 并行配置、依赖规则（默认 PRE 成功才 PROD）。Specs: specs/009-pre-release。测试：并行开关两种模式的编排测试。

## Phase 12 PROD — ⬜
生产发布权限校验、PROD_DEPLOY_SUCCESS→WAIT_PROD_CONFIRM→confirm→COMPLETED、确认角色/超时/代确认配置。Specs: specs/010-production-release。测试：权限拒绝用例、确认幂等、超时。

## Phase 13 Deployment Verification — ⬜
DeploymentAdapter 抽象 + KubernetesAdapter、K8s 多 Pod 成功条件评估器、ReleaseDeploymentNode 记录、HealthCheckConfig 执行器、VersionCheckConfig 执行器、ReleaseSuccessEvaluator。Specs: specs/011-kubernetes-deployment。测试（先写）：4/4 Ready=SUCCESS、3/4=RUNNING、unavailable>0=FAILED、CrashLoopBackOff=FAILED、Timeout=TIMEOUT、Version 不一致=VERSION_CHECK_FAILED。

## Phase 14 Frontend Deployment — ⬜
FrontendAdapter/OSSAdapter/CDNAdapter/StaticServer、HTTP Health + version.json 校验，不依赖 K8s。Specs: specs/012-frontend-deployment。测试：FakeOSS 集成测试。

## Phase 15 Notification — ⬜
NotificationProvider(WeCom/Feishu/Email/Internal)、通知规则（事件×对象×渠道）、MQ 异步发送、发送失败不影响发布状态。Specs: specs/013-notification。测试：FakeNotificationServer、路由规则单测。

## Phase 16 Alert — ⬜
Webhook 接入、Normalize、Fingerprint 去重、频率控制、ACK、升级（level×delay）、恢复通知。Specs: specs/014-alert。测试：1000 次重放→1 Alert；ACK 停普通通知但升级继续；恢复通知内容。

## Phase 17 Admin Configuration — ⬜
管理员配置中心全部资源 API + 配置版本 + 变更对比（当前值/新值/修改人/时间）。Specs: specs/016-admin-config。测试：版本递增、快照不受后续修改影响回归测试。

## Phase 18 Audit — ⬜
操作日志切面覆盖规范 §58 全部动作，before/after 数据。测试：切面集成测试、敏感字段脱敏。

## Phase 19 Observability — ⬜
Micrometer 指标全集、结构化日志字段、Prometheus endpoint。测试：指标存在性断言。

## Phase 20 E2E — ⬜
发布主链路 E2E（计划→测试发布→冲突解决→验收→Release Branch→PRE→PROD→确认→通知）；报警闭环 E2E（webhook→去重→路由→ACK→升级→恢复）。前端 Timeline 页面与管理端页面联调。

## Phase 21 Production Hardening — ⬜
安全加固（CSRF/加密/Webhook Secret 校验）、限流、备份脚本、Helm Chart、压测、故障演练（Jenkins 超时/K8s 卡滚动/MQ 不可用降级）。

---

## 当前执行位置

**Phase 0 待开始。** 每个 Phase 完成时更新本文件状态并输出完成报告。
