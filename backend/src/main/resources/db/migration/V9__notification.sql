-- V9: 通知渠道与路由规则（spec 013-notification，规范 §八/§三十二）
CREATE TABLE notification_channel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL COMMENT '渠道编码，唯一',
    name VARCHAR(64) NOT NULL,
    type VARCHAR(16) NOT NULL COMMENT 'WECOM/FEISHU/EMAIL/INTERNAL',
    config JSON NOT NULL COMMENT '渠道配置（webhook 地址等，secret 加密存储）',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_notification_channel_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知渠道';

CREATE TABLE notification_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL COMMENT 'RELEASE_SUCCESS/RELEASE_FAILED/ALERT/ALERT_ESCALATED/ALERT_RESOLVED/*',
    project_id BIGINT NULL COMMENT 'NULL=全部项目',
    environment_code VARCHAR(16) NULL COMMENT 'NULL=全部环境',
    min_level VARCHAR(16) NOT NULL DEFAULT 'INFO' COMMENT 'INFO/WARN/CRITICAL',
    channel_code VARCHAR(32) NOT NULL,
    receiver_template VARCHAR(255) NOT NULL COMMENT '接收人/webhook 模板（可引用配置占位符）',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_notification_rule_match (event_type, project_id, environment_code, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知路由规则';
