# ADR-009: 事件驱动的异步通知与审计

状态：Accepted | 日期：Phase 0

## 决策
领域事件（ReleaseSucceeded/ReleaseFailed/AlertReceived/Acknowledged/Escalated/Resolved 等）发布到 RocketMQ；Notification Worker 与 Audit Worker 异步消费。

## 理由
- 消息平台故障不得阻塞或回滚发布主流程；通知失败仅记 notification_failed_total + 重试队列，**不得修改已完成的发布状态**。
- 审计解耦，削峰。

## 边界
事务提交后再发消息（Transactional outbox 或事务同步器），避免脏读。消费者幂等（事件 ID 唯一键）。
