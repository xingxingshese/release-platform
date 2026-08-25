package com.company.release.deployment.adapter;

import com.company.release.deployment.verifier.VerifyResult;

/**
 * 统一部署抽象（规范 §六/ADR-005）：K8s 与前端共用同一验证出口，
 * 输出统一 VerifyResult 供节点记录与最终成功判定。
 */
public interface DeploymentAdapter {

    /** K8S / FRONTEND */
    String type();

    /**
     * 验证部署目标是否达到成功条件。
     * 红线（§五）：任何实例异常即整体判败；Jenkins SUCCESS 不参与本判定。
     */
    VerifyOutcome verify(DeploymentTarget target);

    record DeploymentTarget(
            Long taskId,
            String serviceName,
            String type,
            // K8s
            String namespace,
            String deploymentName,
            // 前端
            String healthUrl,
            String versionUrl,
            String expectedVersion) {

        public static DeploymentTarget k8s(Long taskId, String serviceName,
                                           String namespace, String deploymentName) {
            return new DeploymentTarget(taskId, serviceName, "K8S", namespace, deploymentName,
                    null, null, null);
        }

        public static DeploymentTarget frontend(Long taskId, String serviceName,
                                                String healthUrl, String versionUrl, String expectedVersion) {
            return new DeploymentTarget(taskId, serviceName, "FRONTEND",
                    null, null, healthUrl, versionUrl, expectedVersion);
        }
    }

    /** 节点级验证产出：result + 副本/健康/版本明细 + message。 */
    record VerifyOutcome(
            VerifyResult result,
            Integer desired, Integer updated, Integer ready, Integer available, Integer unavailable,
            Boolean healthPassed, String versionExpected, String versionActual, Boolean versionPassed,
            String message) {
    }
}
