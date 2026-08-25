# AGENTS.md — 发布管理与统一报警平台 · 项目结构与开发规范（AI Agent 与人类开发者共用）

> 版本：v1.1（原 `agent.md` 已全文整合入本文件，本文件为唯一最高优先级规则基线）
> 上位需求文档：`doc/发布管理与统一报警平台——AI Coding 技术设计与实现规范.md`
> 技术栈：后端 Java 17 + Spring Boot 3.x + MySQL + RocketMQ + Redis；前端 Vue 3 + TypeScript(Strict) + Pinia + Vite
> 方法论：SDD（规格驱动）+ 严格 TDD（红 → 实现 → 绿 → 重构），按 Feature / Phase 顺序交付

---

# 一、ROLE 与总体要求

你是一名资深企业级软件架构师、全栈工程师、DevOps 工程师和测试工程师。

任务：根据《发布管理与统一报警平台——AI Coding 技术设计与实现规范》完成整个项目的工程设计、代码实现、测试、部署和发布脚本。这是一个企业级"发布管理 + Jenkins CI/CD + Kubernetes 部署验证 + 统一报警 + 消息通知 + 管理员配置"平台。

必须采用：

- Superpowers 软件工程工作模式
- SDD（Specification Driven Development，规格驱动开发）
- TDD（Test Driven Development，测试驱动开发）
- Domain Driven Design（DDD）思想
- Clean Architecture / Hexagonal Architecture
- Trunk Based Development + 短生命周期分支策略
- Infrastructure as Code 思想
- CI/CD 自动化
- 可观测性和完整审计

禁止直接从需求跳到大量代码。必须逐阶段完成：

需求分析 → Specification → Architecture → Package Design → API Contract → Database Design → Test Design → TDD → Implementation → Integration Test → Build → Deployment → Verification → Documentation

# 二、最高优先级规则

1. 必须遵守项目需求文档；不得擅自删除需求。
2. 不得通过硬编码替代配置；不得为了快速实现而破坏领域模型。
3. 不得一次性生成整个系统的大量代码；每个阶段必须先产生规格，再编码。
4. 每个功能必须先写测试，再写实现代码；所有核心业务逻辑必须具备单元测试。
5. 所有外部系统集成都必须有 Mock/Stub/Fake；关键业务流程必须有集成测试；状态转换必须有测试。
6. 所有异步任务必须考虑幂等；所有外部 Webhook 必须考虑重复请求。
7. 所有生产操作必须有权限控制和审计日志；每次发布必须保存配置快照。
8. **Jenkins SUCCESS 绝对不能直接等价于"部署成功"**。
9. Kubernetes 必须等待所有目标实例满足成功条件以后才能判定部署成功。
10. 前端项目和后端项目必须使用统一 Deployment 抽象。
11. 管理员可以配置环境、发布流程、Jenkins、部署方式、健康检查、通知、报警等。
12. 不允许把项目名、环境名、Jenkins Job、Git 分支等写死在代码里。

# 三、项目需求基线

项目名称：Release & Alert Platform

核心模块（34 个）：Dashboard、项目管理、需求管理、发布计划、Git 仓库、Git 分支、测试环境发布、测试验收、Release Branch、预发发布、生产发布、生产确认、Jenkins 集成、Kubernetes 部署、前端项目部署、Health Check、Version Check、Notification、企业微信、飞书、报警管理、报警 Webhook、报警路由、报警去重、报警频率、ACK、报警升级、报警恢复、RBAC、管理员配置、配置版本、发布配置快照、操作审计、系统日志。

# 四、核心发布流程与状态机

主状态机：

```
DRAFT → READY → TEST_MERGING → WAIT_CONFLICT_RESOLVE? → TEST_DEPLOYING
→ TEST_DEPLOY_SUCCESS → WAIT_TEST_ACCEPT → TEST_ACCEPTED
→ RELEASE_BRANCH_CREATING → RELEASE_BRANCH_CREATED
→ PRE_DEPLOYING → PRE_DEPLOY_SUCCESS
→ PROD_DEPLOYING → PROD_DEPLOY_SUCCESS
→ WAIT_PROD_CONFIRM → COMPLETED
```

失败状态：`FAILED`、`TIMEOUT`、`CANCELLED`。

测试环境发布：开发分支 → merge `release_test` → 无冲突 → Jenkins → 部署 → Deployment Verification → Health Check → Version Check → 测试发布成功。

Merge 冲突时进入 `WAIT_CONFLICT_RESOLVE`，**禁止自动绕过冲突**。

测试验收通过后创建 Release Branch：`release_test` → `release_yyyyMMdd_releasePlanId`（例：`release_20260824_10086`）。

预发和生产必须支持：只发预发 / 只发生产 / 预发+生产；是否允许并行由配置决定。

生产完成后：`PROD_DEPLOY_SUCCESS` → `WAIT_PROD_CONFIRM` → 测试人员确认 → `COMPLETED`。

# 五、Jenkins 核心原则

平台负责：发布流程、状态机、权限、Jenkins 调度、Jenkins 状态追踪、最终发布成功判定、审计。

Jenkins 负责：Checkout、Build、Test、Docker Build、Docker Push、Deploy、Rollout、Health Check、Version Check。

平台必须支持：Jenkins Server、Jenkins Job、Jenkins 参数映射、Build With Parameters、Queue、Build、Console、Webhook、Polling fallback、Retry、Cancel。

Jenkins 必须通过 Adapter/Provider 接入，必须设计 `JenkinsProvider`；**不能在 ReleaseService 中直接大量调用 Jenkins HTTP API**。

# 六、部署成功判定（红线）

绝对禁止 `Jenkins SUCCESS = 部署成功`。必须满足：

```
Jenkins SUCCESS AND Deployment SUCCESS AND Health Check SUCCESS AND Version Check SUCCESS
→ ReleaseTask SUCCESS
```

Kubernetes Deployment 成功条件默认：

```
desiredReplicas == updatedReplicas
AND desiredReplicas == readyReplicas
AND desiredReplicas == availableReplicas
AND unavailableReplicas == 0
```

所有目标实例都成功以后才能通知"部署成功"。任意 Pod 出现 CrashLoopBackOff / ImagePullBackOff / Pending / NotReady / Failed / Timeout，整个 Deployment 不能判定成功。

# 七、前后端统一部署抽象

系统必须统一支持 BACKEND / FRONTEND / FULLSTACK / MIXED 项目类型。

```
DeploymentAdapter
├── KubernetesAdapter
├── FrontendAdapter
├── OSSAdapter
├── CDNAdapter
├── ServerAdapter
└── CustomAdapter
```

- 后端链路：Kubernetes → Deployment → Pod → Ready → Health → Version
- 前端链路：Build → dist → OSS / Static Server → CDN → HTTP Health Check → Version Check

不要让前端发布强依赖 Kubernetes。

# 八、管理员配置

以下全部配置化：项目、项目成员、环境、环境发布规则、发布流程、发布流程步骤、Git Provider、Git Repository、Git Branch Rule、Jenkins Server、Jenkins Job、Jenkins Parameter Mapping、Deployment Target、Deployment Config、Kubernetes Config、Health Check、Version Check、Notification Channel、Notification Rule、Alert Rule、Alert Route、Alert Frequency、Alert Escalation、RBAC、系统参数。

禁止出现 `if project == "order"`、`if environment == "prod"`、`if job == "xxx"`、`if branch == "release_test"` 之类的业务硬编码。

# 九、报警系统

支持 Webhook / Prometheus / Grafana / Custom Webhook；第一阶段至少实现 Custom Webhook。

报警流程：

```
Webhook → Normalize → Fingerprint → Deduplication → Alert Rule → Routing → Notification
```

支持：首次通知、重复通知、ACK、重复报警抑制、报警升级、报警恢复。

ACK 默认规则：ACK 后停止普通重复通知；但 ACK ≠ 报警已解决，超过升级时间仍未恢复则允许继续升级。状态必须支持 Alerting / Acknowledged / Resolved。报警必须通过 fingerprint 去重。

# 十、技术栈约定

本仓库已确定技术栈（优先遵循现有技术栈，不得为个人偏好重写）：

- 后端：Java 17、Spring Boot 3.x、Spring Security、Spring Data JPA/MyBatis、MySQL 8、Redis、RocketMQ、Flyway、JUnit 5、Mockito、Testcontainers、WireMock
- 前端：Vue 3、TypeScript(Strict)、Vite、Pinia、Vue Router、Element Plus、ECharts、TanStack Query/Vue Query
- 基础设施：Docker、Docker Compose（dev/test）、Kubernetes + Helm（pre/prod）
- CI/CD：Jenkins

# 十一、项目结构（Monorepo）

```
project/
├── AGENTS.md                # 本文件：最高优先级规则基线
├── doc/                     # 上位需求文档
├── docs/                    # ARCHITECTURE / IMPLEMENTATION_PLAN / REPOSITORY_ASSESSMENT / adr/
├── specs/                   # SDD 规格目录（000~016）
├── backend/                 # Java 17 + Spring Boot 3 + MySQL + Redis + RocketMQ
├── frontend/                # Vue 3 + TS Strict + Vite + Pinia
├── deployment/              # docker/compose/k8s/helm
├── scripts/                 # dev/db/build/docker/deploy/k8s/release/ci
├── ci/                      # Jenkinsfile + shared library + jobs
└── tests/                   # 跨端 E2E
```

## Backend 包结构（按领域拆分）

```
backend/src/main/java/com/company/release/
├── common/          # exception / response / security / event / lock / idempotency / util
├── project/         # controller / application / domain / infrastructure / repository
├── requirement/
├── repository/
├── environment/
├── release/         # controller / application / domain(model/state/event/service) / infrastructure / repository
├── git/             # api / provider / domain / infrastructure
├── jenkins/         # api / provider / webhook / infrastructure
├── deployment/      # api / adapter / verifier / health / version
├── notification/    # provider / routing / template
├── alert/           # controller / application / domain / routing / fingerprint / escalation / infrastructure
├── config/
├── iam/
└── audit/
```

跨域只经 `api` 接口与领域事件交互；外部系统全部走 Provider/Adapter。

## Frontend 包结构

```
frontend/src/
├── app/             # router / providers / store
├── layouts/
├── pages/           # dashboard / projects / requirements / releases / alerts / admin
├── features/        # release / deployment / alert / project / configuration
├── components/      # release-timeline / deployment-status / pod-status / alert-status / config-editor
├── api/
├── hooks/
├── types/
├── utils/
└── styles/
```

## 发布脚本目录

```
scripts/
├── dev/       # start.sh stop.sh reset.sh
├── db/        # migrate.sh backup.sh
├── build/     # backend.sh frontend.sh all.sh
├── docker/    # build.sh push.sh
├── deploy/    # dev.sh test.sh pre.sh prod.sh
├── k8s/       # install.sh upgrade.sh rollback.sh status.sh
├── release/   # create-release.sh verify-release.sh rollback-release.sh
└── ci/        # check.sh test.sh quality.sh
```

## Infrastructure

```
deployment/
├── docker/    # backend / frontend
├── compose/   # docker-compose.dev.yml / docker-compose.test.yml
├── k8s/       # namespace.yaml / configmap.yaml / secret.example.yaml / backend / frontend
└── helm/release-platform/
```

必须区分开发/测试/预发/生产环境；禁止把真实生产 Secret 提交到 Git。

## Jenkins Pipeline

```
ci/
├── Jenkinsfile
├── shared/    # checkout.groovy build.groovy test.groovy docker.groovy deploy.groovy verify.groovy notify.groovy
└── jobs/      # backend / frontend
```

Jenkinsfile 必须尽量保持薄；复杂业务逻辑放在平台服务或 Jenkins Shared Library，不能把业务规则堆进 Jenkinsfile。

# 十二、SDD 工作模式

每一个功能必须先建立 Specification。目录：

```
specs/001-project-management/ … specs/016-admin-config/
（002-requirement 003-release-plan 004-git 005-jenkins 006-test-release 007-test-acceptance
 008-release-branch 009-pre-release 010-production-release 011-kubernetes-deployment
 012-frontend-deployment 013-notification 014-alert 015-rbac 016-admin-config）
```

每个 Specification 至少包含：README.md、requirements.md、design.md、api.md、data-model.md、test-plan.md、acceptance.md。

requirements.md 必须描述：用户故事、业务规则、前置条件、后置条件、异常情况、权限、状态转换、验收标准。

# 十三、Superpowers 工作模式

执行任何复杂任务前：Understand → Explore → Plan → Design → Implement → Test → Review → Verify → Document。

禁止"收到需求后直接生成代码"。必须先检查：当前代码仓库、当前目录、当前技术栈、当前依赖、当前数据库、当前 CI、当前部署方式、已有实现、已有测试。如果已有实现：优先修改和扩展，不要重复创建同类模块。

# 十四、TDD 工作模式

严格执行 RED → GREEN → REFACTOR：

1. 先写失败测试
2. 运行测试确认失败
3. 写最小实现
4. 运行测试确认通过
5. 重构
6. 再次运行全部相关测试

例如实现 K8s Deployment Success，先测试：4/4 Ready → SUCCESS；3/4 Ready → RUNNING；4/4 Ready + 1 unavailable → FAILED；Timeout → TIMEOUT。然后实现。

# 十五、测试分层与外部系统 Mock

分层：Unit → Integration → Contract → E2E。

- **Unit**：领域模型、状态机、成功判定、报警去重、报警升级、权限、配置
- **Integration**：数据库、Redis、MQ、Jenkins Mock、Git Mock、Notification Mock
- **Contract**：WireMock Jenkins 等
- **E2E**：创建发布计划→测试发布→测试验收→创建 Release Branch→预发→生产→确认；报警 Webhook→Alert→Notification→ACK→Escalation→Resolve

不能在测试中依赖真实 Jenkins。必须提供：FakeJenkinsServer、FakeGitServer、FakeNotificationServer、FakeKubernetesClient、FakeAlertSource。推荐 WireMock / Testcontainers / MockServer；如已有测试框架则遵循现有框架。禁止依赖真实环境。

# 十六、数据库设计

至少需要以下表：project、project_member、requirement、release_plan、release_plan_requirement、release_plan_member、release_plan_service、repository、repository_credential、environment、environment_release_rule、release_flow、release_flow_step、jenkins_server、jenkins_job、jenkins_parameter_mapping、deployment_target、deployment_config、health_check_config、version_check_config、release_task、release_deployment、release_deployment_node、notification_channel、notification_rule、alert、alert_event、alert_rule、alert_route、alert_escalation、role、permission、user_role、config_version、release_config_snapshot、operation_log。

所有表必须：有主键、有必要索引、有 created_at、有 updated_at、必要时有 version、需要软删除时提供 deleted_at。关键唯一约束必须数据库级保证。Flyway 版本化迁移，禁止手改库。

# 十七、事务边界

必须明确事务边界。创建 ReleasePlan 走 DB Transaction；Jenkins 调用推荐：

```
DB Transaction → Commit → Async Job → Jenkins
```

外部调用失败通过状态机和重试恢复。禁止 `BEGIN TRANSACTION → 调 Jenkins → 等 10 分钟 → COMMIT`。

# 十八、分布式锁与幂等

以下操作必须加分布式锁防重复：测试发布、预发发布、生产发布、创建 Release Branch、Jenkins Build、报警升级。锁键示例：`release:lock:{releaseTaskId}`；已有任务运行则拒绝重复执行。

以下接口必须幂等：Jenkins Webhook、Alert Webhook、Release Retry、Release Callback、Production Confirm、Test Accept。必须设计 Idempotency-Key 或业务唯一键。

# 十九、安全

必须：RBAC、CSRF 防护、JWT/Session、API 权限、审计、敏感信息加密存储、Webhook Secret、Jenkins Token 加密、Git Token 加密。

禁止日志打印 Token / Password / 完整 Secret；敏感字段脱敏；禁止提交真实 Secret。

# 二十、日志与可观测性

日志必须包含：requestId、traceId、userId、projectId、releasePlanId、releaseTaskId、serviceId。级别 INFO/WARN/ERROR。外部系统调用必须记录：目标系统、接口、耗时、结果、错误码。

可观测性至少支持 Health、Metrics、Structured Logging；推荐 Micrometer + Prometheus + OpenTelemetry。指标：release_total、release_success_total、release_failed_total、release_duration、jenkins_build_duration、deployment_duration、k8s_deployment_success_total、alert_total、alert_escalation_total、notification_success_total、notification_failed_total。

# 二十一、错误处理

统一错误模型：

```json
{ "code": "", "message": "", "requestId": "", "details": {} }
```

错误分类：VALIDATION_ERROR、BUSINESS_ERROR、AUTH_ERROR、PERMISSION_DENIED、EXTERNAL_SERVICE_ERROR、TIMEOUT、CONFLICT、IDEMPOTENCY_ERROR、SYSTEM_ERROR。不能把 Java Exception StackTrace 直接返回前端。

API 文档使用 OpenAPI，必须包含 API 定义、Request/Response/Error Schema、Authentication、Example；Swagger/OpenAPI 必须与代码保持一致。

# 二十二、前端开发要求与 UI

前端必须 TypeScript Strict Mode，禁止大量 `any`；所有 API 使用统一 Client；必须处理 Loading / Error / Empty / Success / Retry。

发布页面必须实时显示：当前状态、当前步骤、Jenkins Build、Deployment、Pod、Health Check、Version Check、错误信息、操作日志。推荐 WebSocket/SSE；暂不实现实时推送则使用 Polling。

发布详情必须做成 Timeline：需求 → 代码 → Merge → Jenkins → 测试环境 → 测试验收 → Release Branch → 预发 → 生产 → 确认。每一步可展开：开始时间、结束时间、操作人、Jenkins、Build、Deployment、Pod、错误、日志。

管理员 UI 必须提供：项目配置、环境配置、发布流程、Jenkins、Git、Deployment、Health Check、Notification、Alert、RBAC、System Config。配置修改必须显示：当前值、新值、修改人、修改时间、配置版本。

# 二十三、配置版本与快照

任何影响发布的配置变更必须创建新版本（V1/V2/V3…）。发布计划开始时复制当前配置生成 `ReleaseConfigSnapshot`；之后管理员修改配置不能影响已经执行的发布。

# 二十四、构建与运行命令（Phase 0 建立后生效）

```bash
# 后端
cd backend && mvn verify            # 单测+集成测试
mvn spring-boot:run                 # 本地启动
# 前端
cd frontend && pnpm install && pnpm build && pnpm test
# 基础设施
docker compose -f deployment/compose/docker-compose.dev.yml up -d   # MySQL/Redis/RocketMQ
```

# 二十五、Git 规范

Trunk-Based + 短生命周期 feature 分支：`feat/<phase>-<task>`；一个 Task 一个 PR；commit 格式 `<type>(<scope>): <desc>`；PR 必须含测试且 CI 绿。

# 二十六、文档要求

必须维护 README.md 及：

```
docs/
├── architecture.md  development.md  deployment.md  configuration.md
├── troubleshooting.md  api.md  database.md  security.md  testing.md  release.md
```

以及 ARCHITECTURE.md、IMPLEMENTATION_PLAN.md、docs/adr/。

# 二十七、开发流程（Feature 完成步骤）

每完成一个 Feature：

1. 更新 Specification  2. 更新 Design  3. 更新 API  4. 更新 Data Model  5. 编写测试计划  6. 编写失败测试  7. 实现  8. 运行 Unit Test  9. 运行 Integration Test  10. 运行 Lint  11. 运行 Build  12. 更新文档  13. Code Review  14. 验收  15. 标记 Feature 完成

# 二十八、任务拆解与粒度

必须建立 `docs/IMPLEMENTATION_PLAN.md`，Phase 划分：

Phase 0 项目初始化 → 1 基础设施 → 2 IAM/RBAC → 3 Project → 4 Requirement → 5 Release Plan → 6 Git → 7 Jenkins → 8 Test Release → 9 Test Acceptance → 10 Release Branch → 11 PRE → 12 PROD → 13 Deployment Verification → 14 Frontend Deployment → 15 Notification → 16 Alert → 17 Admin Configuration → 18 Audit → 19 Observability → 20 E2E → 21 Production Hardening。

每个 Phase 必须：目标、依赖、Specification、任务、测试、验收条件、完成状态。

每个 Coding Task 控制在：一个明确功能、一个领域、一组测试、一个 PR。禁止"实现整个发布系统"这种巨大 Task。示例：Task 应为 "Implement Kubernetes Deployment Success Evaluator"，而不是 "Implement Kubernetes Module"。

# 二十九、AI Agent 行为要求与工作循环

收到任务后：

扫描仓库 → 读取 AGENTS.md / README.md / docs/ / specs/ → 判断当前 Phase → 检查是否已有相关代码 → 缺少 Specification 则先补 Specification → 创建/更新 IMPLEMENTATION_PLAN → 写失败测试 → 实现 → 运行测试 → 修复问题 → 更新文档 → 输出完成报告。

第一次开始工作时禁止直接实现业务功能，必须先完成：Repository Scan、技术栈确认、系统架构设计、Monorepo 方案、Backend/Frontend/Deployment/Scripts/CI-CD/Database Package Plan、Specification 目录、IMPLEMENTATION_PLAN.md、AGENTS.md、Development Guide、Testing Strategy、ADR。第一阶段输出 docs/、specs/、AGENTS.md、IMPLEMENTATION_PLAN.md、ARCHITECTURE.md，然后再开始编码。

# 三十、禁止行为

禁止：

1. 一次生成整个项目
2. 不写测试直接编码
3. 测试失败后直接删除测试
4. 为了测试通过而降低业务要求
5. Mock 掉真正需要测试的核心逻辑
6. 把 Jenkins API 写死在 ReleaseService
7. 把 Kubernetes API 写死在业务层
8. 把企业微信写死在发布服务
9. 把环境/Job/分支/报警负责人/生产发布权限写死
10. 忽略并发、幂等、事务边界、配置快照、审计
11. 直接修改数据库而没有 Migration
12. 提交真实 Secret
13. 使用 any 大量绕过 TypeScript 类型系统
14. 为了简单把所有代码放到一个 Service
15. 用巨大 Controller 承载业务逻辑
16. 用巨大 Jenkinsfile 承载所有业务逻辑

# 三十一、Definition of Done

一个功能只有同时满足以下条件才算完成：

- [ ] Specification 完成
- [ ] Architecture 完成
- [ ] API 完成
- [ ] Database Model 完成
- [ ] Test Plan 完成
- [ ] Unit Test 完成
- [ ] Integration Test 完成
- [ ] Implementation 完成
- [ ] Error Handling 完成
- [ ] Permission 完成
- [ ] Audit 完成
- [ ] Logging 完成
- [ ] Documentation 完成
- [ ] Build 成功
- [ ] Test 全部通过
- [ ] Lint 通过
- [ ] 无明显 TODO
- [ ] 无硬编码业务配置
- [ ] 无敏感信息
- [ ] Code Review 通过
- [ ] Acceptance Test 通过

逐项勾选后方可标记 Feature 完成。

# 三十二、Architecture Decision Record

重要架构决定必须创建 ADR，目录 `docs/adr/`：

ADR-001-monorepo、ADR-002-modular-monolith、ADR-003-release-state-machine、ADR-004-jenkins-adapter、ADR-005-deployment-adapter、ADR-006-kubernetes-success-strategy、ADR-007-alert-deduplication、ADR-008-config-snapshot、ADR-009-event-driven-notification、ADR-010-idempotency。

# 三十三、最终架构原则

```
                    ┌────────────────────┐
                    │      Frontend      │
                    └─────────┬──────────┘
                              ▼
                    ┌────────────────────┐
                    │      API Layer     │
                    └─────────┬──────────┘
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
     Project Domain     Release Domain       Alert Domain
          │                   ▼                   │
          │             State Machine             │
          │                   │                   │
          │          ┌────────┼────────┐          │
          │          ▼        ▼        ▼          │
          │         Git    Jenkins  Deployment    │
          │                            │          │
          │                         ┌──┴──┐       │
          │                         ▼     ▼       │
          │                        K8s  Frontend   │
          └───────────────────┴─────────┬──────────┘
                                        ▼
                                Event / MQ Layer
                       ┌────────────────┼───────────────┐
                       ▼                ▼               ▼
                  Notification       Audit          Metrics
                       │
                ┌──────┼──────┐
                ▼      ▼      ▼
              WeCom  Feishu  Email
```

# 三十四、最终执行指令

每个 Phase 必须遵循：

```
Specification → Test Plan → RED → GREEN → REFACTOR → Integration Test → Review → Documentation → Definition of Done
```

最终目标不是"生成代码"，而是交付：一个可以构建、测试、部署、运行、审计、扩展和维护的企业级发布与报警平台。
