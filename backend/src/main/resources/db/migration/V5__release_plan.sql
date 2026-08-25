-- V5: 发布计划域（规范 §8/§9/§10/§42）
CREATE TABLE release_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    version_name VARCHAR(64) NULL COMMENT '业务版本号，如 2026.08.24',
    description VARCHAR(1024) NULL,
    release_owner_id BIGINT NOT NULL,
    planned_time DATETIME NULL,
    status VARCHAR(48) NOT NULL DEFAULT 'DRAFT',
    environments VARCHAR(128) NOT NULL DEFAULT 'TEST' COMMENT '选择发布的环境组合：TEST/PRE/PROD 逗号分隔',
    config_snapshot_id BIGINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_plan_project_status (project_id, status),
    KEY idx_plan_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE release_plan_requirement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    release_plan_id BIGINT NOT NULL,
    requirement_id BIGINT NOT NULL,
    UNIQUE KEY uk_plan_req (release_plan_id, requirement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE release_plan_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    release_plan_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL COMMENT 'DEVELOPER/TESTER/PRODUCT/RELEASE_OWNER',
    UNIQUE KEY uk_plan_member (release_plan_id, user_id, role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE release_plan_service (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    release_plan_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    repository_id BIGINT NOT NULL,
    source_branch VARCHAR(128) NOT NULL,
    target_test_branch VARCHAR(128) NOT NULL,
    release_branch VARCHAR(128) NULL,
    commit_id VARCHAR(64) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_plan_service (release_plan_id, service_id),
    KEY idx_rps_plan (release_plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE release_config_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content JSON NOT NULL COMMENT '流程/环境/Jenkins/Git/部署/健康检查/通知等配置快照',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发布配置快照（ADR-008）';

CREATE TABLE release_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    release_plan_id BIGINT NOT NULL,
    environment_code VARCHAR(32) NOT NULL COMMENT 'TEST/PRE/PROD 等（配置化环境 code）',
    status VARCHAR(48) NOT NULL DEFAULT 'PENDING',
    jenkins_server_id BIGINT NULL,
    jenkins_job_name VARCHAR(255) NULL,
    jenkins_queue_id BIGINT NULL,
    jenkins_build_number BIGINT NULL,
    error_message VARCHAR(1024) NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_plan_env (release_plan_id, environment_code),
    KEY idx_task_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
