-- V7: Jenkins 配置（规范 §20-§22）
CREATE TABLE jenkins_server (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    url VARCHAR(255) NOT NULL,
    credential_id BIGINT NULL COMMENT '凭证加密存储',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_jenkins_server_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE jenkins_job (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    environment_code VARCHAR(32) NOT NULL COMMENT 'TEST/PRE/PROD 等',
    jenkins_server_id BIGINT NOT NULL,
    job_name VARCHAR(255) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_jenkins_job (project_id, service_id, environment_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='service × environment → job 的配置映射，禁止硬编码';

CREATE TABLE jenkins_parameter_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    jenkins_job_id BIGINT NOT NULL,
    platform_field VARCHAR(64) NOT NULL,
    jenkins_parameter VARCHAR(64) NOT NULL,
    required TINYINT(1) NOT NULL DEFAULT 0,
    default_value VARCHAR(128) NULL,
    UNIQUE KEY uk_param_mapping (jenkins_job_id, platform_field)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
