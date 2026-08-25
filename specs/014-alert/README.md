# 014-alert

状态：⬜ 待 Phase 16 填充
范围：POST /api/v1/alerts/webhook/{projectKey}、Normalize、fingerprint 去重、频率控制（首次立即/重复间隔/最大次数）、ACK（停普通重复但升级继续）、Escalation(level/delay/receivers/channels)、恢复通知、AlertProvider 抽象（第一期 Custom Webhook）（规范 §45-§52）。ADR-007。
