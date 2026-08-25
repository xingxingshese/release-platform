package com.company.release.deployment.adapter;

import com.company.release.deployment.domain.KubernetesDeploymentSnapshot;

/**
 * K8s 快照获取端口（规范 §二十：端口抽象隔离真实 Client，测试注入 Fake）。
 * 生产实现经 Kubernetes API（RestClient/fabric8）；本阶段由部署方实现接入。
 */
public interface KubernetesSnapshotFetcher {

    KubernetesDeploymentSnapshot fetch(String namespace, String deploymentName);
}
