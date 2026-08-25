# AGENTS.md — 项目结构与开发规范（AI Agent 与人类开发者共用）

> 上位规则：`agent.md`（最高优先级）+ `doc/发布管理与统一报警平台——AI Coding 技术设计与实现规范.md`

## 1. 项目结构（Monorepo）

```
project/
├── agent.md / doc/          # 需求与规则基线
├── docs/                    # ARCHITECTURE / IMPLEMENTATION_PLAN / REPOSITORY_ASSESSMENT / adr/
├── specs/                   # SDD 规格目录（000~016）
├── backend/                 # Java 17 + Spring Boot 3 + MySQL + Redis + RocketMQ
├── frontend/                # Vue 3 + TS Strict + Vite + Pinia
├── deployment/              # docker/compose/k8s/helm
├── scripts/                 # dev/db/build/docker/deploy/k8s/release/ci
├── ci/                      # Jenkinsfile + shared library + jobs
└── tests/                   # 跨端 E2E
```

## 2. 开发规范

- **SDD 先行**：编码前对应 `specs/0xx-*/` 必须有 requirements/design/api/data-model/test-plan。
- **TDD 强制**：RED→GREEN→REFACTOR；先写失败测试再实现；禁止删测试或降标准求通过。
- **架构**：模块化单体，按领域分包；跨域只经 `api` 接口与领域事件；外部系统全部 Provider/Adapter（Jenkins/Git/K8s/OSS/通知/需求源）。详见 `docs/ARCHITECTURE.md` 与 ADR-001~010。
- **禁止硬编码**：项目/环境/Job/分支/负责人/权限一律走配置 + 快照；禁止 `if env=="prod"` 类逻辑。
- **成功判定红线**：Jenkins SUCCESS ≠ 部署成功；必须 Deployment+Health+Version 全部通过才 SUCCESS，之后才允许通知。K8s 条件见 ADR-006。
- **并发/幂等/事务**：发布操作加分布式锁；Webhook 与重试类接口幂等（ADR-010）；外部调用不得包在长事务内。
- **安全**：RBAC；凭证加密存储；日志脱敏；禁止提交真实 Secret。
- **数据库**：Flyway 版本化迁移，禁止手改库。

## 3. 测试规范

分层：Unit（状态机/成功判定/去重/升级/权限）→ Integration（DB/Redis/MQ/Fake 外部系统）→ Contract（WireMock Jenkins 等）→ E2E（发布主链路、报警闭环）。外部系统一律用 Fake/WireMock/Testcontainers，禁止依赖真实环境。

## 4. 构建与运行命令（Phase 0 建立后生效）

```bash
# 后端
cd backend && mvn verify            # 单测+集成测试
mvn spring-boot:run                 # 本地启动
# 前端
cd frontend && pnpm install && pnpm build && pnpm test
# 基础设施
docker compose -f deployment/compose/docker-compose.dev.yml up -d   # MySQL/Redis/RocketMQ
```

## 5. Git 规范

Trunk-Based + 短生命周期 feature 分支：`feat/<phase>-<task>`；一个 Task 一个 PR；commit 格式 `<type>(<scope>): <desc>`；PR 必须含测试且 CI 绿。

## 6. AI Agent 工作循环

收到任务 → 扫描仓库 → 判断当前 Phase → 补 Specification → 更新 IMPLEMENTATION_PLAN → 写失败测试 → 实现 → 全量测试 → 更新文档 → 输出完成报告。禁止一次性生成整个系统的代码。

## 7. Definition of Done

见 `agent.md` §四十 清单，逐项勾选后方可标记 Feature 完成。
