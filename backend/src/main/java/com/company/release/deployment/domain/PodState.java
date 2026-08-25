package com.company.release.deployment.domain;

/**
 * 单个部署实例（Pod）状态。reason 用于记录 CrashLoopBackOff / ImagePullBackOff 等原因。
 * 状态枚举对应规范 §29：PENDING/STARTING/RUNNING/READY/FAILED/TIMEOUT。
 */
public record PodState(String status, String reason) {

    public boolean isReady() {
        return "READY".equals(status);
    }

    public boolean isFailed() {
        return "FAILED".equals(status) || "TIMEOUT".equals(status)
                || (reason != null && (reason.contains("CrashLoopBackOff") || reason.contains("ImagePullBackOff")));
    }
}
