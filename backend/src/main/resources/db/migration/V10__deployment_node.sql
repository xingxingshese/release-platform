-- V10: 发布部署节点记录（spec 011-kubernetes-deployment / 012-frontend-deployment）
CREATE TABLE release_deployment_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    release_task_id BIGINT NOT NULL,
    service_name VARCHAR(128) NOT NULL,
    node_name VARCHAR(128) NOT NULL COMMENT 'Pod 实例名或前端部署目标名',
    deployment_type VARCHAR(16) NOT NULL COMMENT 'K8S/FRONTEND',
    replica_desired INT NULL,
    replica_updated INT NULL,
    replica_ready INT NULL,
    replica_available INT NULL,
    replica_unavailable INT NULL,
    health_passed TINYINT(1) NULL,
    version_expected VARCHAR(64) NULL,
    version_actual VARCHAR(64) NULL,
    version_passed TINYINT(1) NULL,
    result VARCHAR(32) NOT NULL COMMENT 'SUCCESS/RUNNING/FAILED/TIMEOUT/VERSION_CHECK_FAILED',
    message VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_release_deployment_node_task (release_task_id),
    KEY idx_release_deployment_node_result (result)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发布部署节点逐实例判定明细';
