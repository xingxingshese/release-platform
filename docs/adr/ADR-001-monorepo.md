# ADR-001: 采用 Monorepo

状态：Accepted | 日期：Phase 0

## 背景
平台含后端、前端、部署清单、脚本、CI 配置，需同步演进。

## 决策
采用 Monorepo：`backend/ frontend/ deployment/ deploy/ scripts/ ci/ docker/ helm/ k8s/ tools/ examples/ tests/ specs/ docs/`，根目录 Jenkinsfile、docker-compose.yml、Makefile、README.md、AGENTS.md。

## 理由
单人/小团队迭代快；前后端 API 契约变更原子提交；specs 与代码同仓可追溯。

## 后果
需要 CI 按路径过滤触发构建；仓库变大后可再拆分。
