package com.company.release.deployment.verifier;

import com.company.release.deployment.domain.KubernetesDeploymentSnapshot;
import com.company.release.deployment.domain.PodState;

/**
 * K8s Deployment 成功判定器（ADR-006 核心红线）。
 *
 * 成功条件：
 *   desired == updated && desired == ready && desired == available && unavailable == 0
 *   且所有 Pod READY，无 FAILED/超时实例。
 *
 * Jenkins SUCCESS 绝不等价于部署成功——本判定器是唯一事实来源。
 */
@org.springframework.stereotype.Component
public class KubernetesDeploymentVerifier {

    public VerifyResult verify(KubernetesDeploymentSnapshot snapshot) {
        if (snapshot.timedOut()) {
            return VerifyResult.TIMEOUT;
        }
        boolean anyPodFailed = snapshot.pods().stream().anyMatch(p ->
                p.isFailed() || ("PENDING".equals(p.status())
                        && p.reason() != null && !p.reason().isBlank()));
        if (anyPodFailed) {
            return VerifyResult.FAILED;
        }

        boolean replicaCondition =
                snapshot.desiredReplicas() == snapshot.updatedReplicas()
                        && snapshot.desiredReplicas() == snapshot.readyReplicas()
                        && snapshot.desiredReplicas() == snapshot.availableReplicas()
                        && snapshot.unavailableReplicas() == 0;

        // 副本数收敛但存在不可用实例（如旧实例未退出）→ 判定失败（ADR-006 用例 3）
        boolean convergedButUnavailable =
                snapshot.desiredReplicas() == snapshot.updatedReplicas()
                        && snapshot.desiredReplicas() == snapshot.readyReplicas();
        if (convergedButUnavailable && !replicaCondition) {
            return VerifyResult.FAILED;
        }

        if (replicaCondition) {
            boolean allPodsReady = snapshot.pods().stream().allMatch(PodState::isReady);
            return allPodsReady ? VerifyResult.SUCCESS : VerifyResult.FAILED;
        }

        // 存在 unavailable>0 且长时间不收敛的情况由上层 timeout 控制转 TIMEOUT；
        // 未超时且未失败 → 滚动进行中
        return VerifyResult.RUNNING;
    }
}
