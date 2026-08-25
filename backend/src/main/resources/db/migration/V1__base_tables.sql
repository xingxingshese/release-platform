-- V1: 公共基础表（规范 §58 operation_log、ADR-008 config_version）
CREATE TABLE operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    module VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NULL,
    target_id VARCHAR(64) NULL,
    request_id VARCHAR(64) NULL,
    before_data JSON NULL,
    after_data JSON NULL,
    ip VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_operation_log_module_action (module, action),
    KEY idx_operation_log_target (target_type, target_id),
    KEY idx_operation_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

CREATE TABLE config_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_type VARCHAR(64) NOT NULL COMMENT '配置类型：environment/jenkins/deployment/... ',
    config_key VARCHAR(128) NOT NULL COMMENT '配置标识',
    version INT NOT NULL COMMENT '版本号，同 key 递增',
    content JSON NOT NULL COMMENT '配置内容快照',
    changed_by BIGINT NOT NULL,
    change_reason VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_config_version (config_type, config_key, version),
    KEY idx_config_version_key (config_type, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置版本';
