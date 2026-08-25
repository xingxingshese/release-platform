-- V8: 报警中心（规范 §45-§52）
CREATE TABLE alert (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    project_key VARCHAR(64) NULL COMMENT 'Webhook 路径键',
    source VARCHAR(32) NOT NULL DEFAULT 'CUSTOM_WEBHOOK',
    external_alert_id VARCHAR(128) NULL,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(2048) NULL,
    level VARCHAR(16) NOT NULL COMMENT 'INFO/WARNING/ERROR/CRITICAL',
    status VARCHAR(24) NOT NULL DEFAULT 'ALERTING' COMMENT 'ALERTING/ACKNOWLEDGED/RESOLVED',
    environment VARCHAR(64) NULL,
    service VARCHAR(128) NULL,
    labels JSON NULL,
    fingerprint CHAR(64) NOT NULL,
    notified_repeat_count INT NOT NULL DEFAULT 0,
    escalated_to_level INT NOT NULL DEFAULT 0,
    first_occurred_at DATETIME(3) NOT NULL,
    last_occurred_at DATETIME(3) NOT NULL,
    acknowledged_by BIGINT NULL,
    acknowledged_at DATETIME NULL,
    resolved_at DATETIME NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_alert_project_fp (project_id, fingerprint),
    KEY idx_alert_status_level (status, level),
    KEY idx_alert_last_occurred (last_occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notification_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel VARCHAR(32) NOT NULL COMMENT 'WECOM/FEISHU/EMAIL/INTERNAL',
    receiver VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    related_type VARCHAR(32) NULL,
    related_id VARCHAR(64) NULL,
    success TINYINT(1) NOT NULL DEFAULT 1,
    error_message VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_notification_related (related_type, related_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
