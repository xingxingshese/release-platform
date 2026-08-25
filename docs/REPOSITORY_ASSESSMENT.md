# Repository Assessment（仓库现状评估）

> 评估时间：Phase 0 开始时
> 评估人：AI Coding Agent（ox-alpha）

## 1. 扫描结果

| 项目 | 结果 |
|---|---|
| 已有业务代码 | 无 |
| 后端工程（pom.xml / build.gradle） | 无 |
| 前端工程（package.json / vite.config.*） | 无 |
| 数据库 / Migration 脚本 | 无 |
| CI/CD 配置（Jenkinsfile、GitHub Actions） | 无 |
| Docker / K8s / Helm 清单 | 无 |
| 已有测试 | 无 |
| 已有规范文档 | `doc/发布管理与统一报警平台——AI Coding 技术设计与实现规范.md`、`agent.md` |

## 2. 结论

- **绿地项目（Greenfield）**：不存在已有技术栈约束，可按规范推荐的默认技术栈执行。
- 需求基线以《技术设计与实现规范》为准，`agent.md` 为最高优先级工作规则。
- 无历史数据库，可直接采用 Flyway V1 起步的版本化 Migration。

## 3. 确定的技术栈

| 层 | 选型 | 依据 |
|---|---|---|
| 语言/JDK | Java 17 + Spring Boot 3.x | agent.md 头部指定 |
| ORM | Spring Data JPA + Flyway | 规范 §9、§69.24 |
| 数据库 | MySQL 8 | agent.md 指定 |
| 缓存/锁 | Redis | 去重、分布式锁、幂等（规范 §53） |
| MQ | RocketMQ | agent.md 指定；事件驱动通知/审计 |
| 安全 | Spring Security + JWT + RBAC | 规范 §56 |
| API 文档 | springdoc-openapi | 规范 §29 |
| 测试 | JUnit 5 + Mockito + Testcontainers + WireMock | agent.md §十九/§二十 |
| 前端 | Vue 3 + TypeScript(Strict) + Vite + Pinia + Element Plus + ECharts | agent.md 头部、§三十 |
| 实时推送 | 先 Polling，后 SSE/WebSocket | 规范 §30 允许降级 |
| 构建 | Maven（后端）、pnpm（前端） | — |
| 部署 | Docker Compose（dev/test）→ Helm/K8s（pre/prod） | agent.md §十四 |

## 4. 仓库策略

**Monorepo**（ADR-001）：`backend/` + `frontend/` + `deployment/` + `scripts/` + `ci/` + `specs/` + `docs/`。

## 5. 下一步

见 `IMPLEMENTATION_PLAN.md`，从 Phase 0（项目脚手架初始化）开始执行。
