package com.company.release.deployment.domain;

import java.util.List;

/**
 * K8s 工作负载状态快照（Deployment/StatefulSet 等）。
 */
public record KubernetesDeploymentSnapshot(
        int desiredReplicas,
        int updatedReplicas,
        int readyReplicas,
        int availableReplicas,
        int unavailableReplicas,
        List<PodState> pods,
        boolean timedOut
) {
    public KubernetesDeploymentSnapshot {
        pods = pods == null ? List.of() : List.copyOf(pods);
    }
}
