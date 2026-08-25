# ADR-002: 模块化单体架构

状态：Accepted | 日期：Phase 0

## 背景
规范 §2 建议第一期模块化单体，避免过早微服务化。

## 决策
单一 Spring Boot 进程，按领域分包（project/requirement/release/git/jenkins/deployment/notification/alert/config/iam/audit/common）。跨域仅通过 `api` 接口与领域事件交互；domain 层不依赖框架细节。

## 理由
降低运维成本；保留按 `release/alert/notification/deployment/config` 拆分微服务的演进路径。

## 后果
需要 ArchUnit 规则约束包依赖方向（后续 Phase 引入）。
