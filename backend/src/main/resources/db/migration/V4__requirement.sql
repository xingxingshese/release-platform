-- V4: 需求管理（规范 §6）
CREATE TABLE requirement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1024) NULL,
    source_type VARCHAR(32) NOT NULL COMMENT 'MANUAL/YUNXIAO/JIRA/TAPD/OTHER',
    external_id VARCHAR(128) NULL COMMENT '外部系统需求 ID',
    external_url VARCHAR(512) NULL,
    owner_id BIGINT NULL,
    priority VARCHAR(16) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_requirement_external (project_id, source_type, external_id),
    KEY idx_requirement_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
