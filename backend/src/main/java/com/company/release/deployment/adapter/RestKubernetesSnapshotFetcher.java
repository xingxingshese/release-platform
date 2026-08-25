package com.company.release.deployment.adapter;

import com.company.release.deployment.domain.KubernetesDeploymentSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * 生产 K8s 快照拉取实现：经 Kubernetes API 读取 Deployment 状态。
 * 未配置 K8S_API_URL 时返回 timedOut 快照（绝不伪造成功，红线 ADR-006）。
 */
@Component
public class RestKubernetesSnapshotFetcher implements KubernetesSnapshotFetcher {

    @Value("${k8s.api-url:}")
    private String apiUrl;

    @Value("${k8s.token:}")
    private String token;

    @Override
    public KubernetesDeploymentSnapshot fetch(String namespace, String deploymentName) {
        if (apiUrl == null || apiUrl.isBlank()) {
            // 未接入集群：返回超时空快照（判定 TIMEOUT 而非 SUCCESS）
            return new KubernetesDeploymentSnapshot(1, 0, 0, 0, 1, List.of(), true);
        }
        var client = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
        var status = client.get()
                .uri("/apis/apps/v1/namespaces/{ns}/deployments/{name}/status", namespace, deploymentName)
                .retrieve()
                .body(StatusResponse.class);
        if (status == null || status.status() == null) {
            return new KubernetesDeploymentSnapshot(1, 0, 0, 0, 1, List.of(), true);
        }
        var s = status.status();
        List<com.company.release.deployment.domain.PodState> pods = List.of();
        return new KubernetesDeploymentSnapshot(
                nz(s.replicas()), nz(s.updatedReplicas()), nz(s.readyReplicas()),
                nz(s.availableReplicas()), nz(s.unavailableReplicas()), pods, false);
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    /** 最小化响应映射（只取 status 计数）。 */
    record StatusResponse(Status status) {
        record Status(Integer replicas, Integer updatedReplicas, Integer readyReplicas,
                      Integer availableReplicas, Integer unavailableReplicas) {
        }
    }
}
