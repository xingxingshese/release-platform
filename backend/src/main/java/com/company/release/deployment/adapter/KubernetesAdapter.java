package com.company.release.deployment.adapter;

import com.company.release.deployment.verifier.KubernetesDeploymentVerifier;
import org.springframework.stereotype.Component;

/**
 * Kubernetes 适配器（ADR-005/006）：拉取 Deployment 快照 → ADR-006 成功条件判定。
 * K8s API 异常向上抛出，由调用方判 FAILED，绝不静默当作成功。
 */
@Component
public class KubernetesAdapter implements DeploymentAdapter {

    private final KubernetesSnapshotFetcher fetcher;
    private final KubernetesDeploymentVerifier verifier = new KubernetesDeploymentVerifier();

    public KubernetesAdapter(KubernetesSnapshotFetcher fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public String type() {
        return "K8S";
    }

    @Override
    public VerifyOutcome verify(DeploymentTarget target) {
        var snapshot = fetcher.fetch(target.namespace(), target.deploymentName());
        var result = verifier.verify(snapshot);
        boolean allReady = result == com.company.release.deployment.verifier.VerifyResult.SUCCESS;
        return new VerifyOutcome(
                result,
                snapshot.desiredReplicas(), snapshot.updatedReplicas(),
                snapshot.readyReplicas(), snapshot.availableReplicas(),
                snapshot.unavailableReplicas(),
                allReady, null, null, null,
                "k8s rollout verify");
    }
}
