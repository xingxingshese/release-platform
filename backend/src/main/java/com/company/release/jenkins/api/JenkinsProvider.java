package com.company.release.jenkins.api;

import java.util.Map;

/**
 * Jenkins Provider 统一接口（规范 §19/§23）。实现不得与发布编排耦合。
 * 状态追踪：Webhook 主通道 + Polling 兜底（Phase 8 编排）。
 */
public interface JenkinsProvider {

    enum BuildStatus {
        QUEUED, RUNNING, SUCCESS, FAILURE, ABORTED, UNKNOWN
    }

    /**
     * Build With Parameters：提交构建，返回 queueId。
     */
    long buildWithParameters(String jobName, Map<String, String> parameters);

    /** Queue → Build Number（排队中返回 null）。 */
    Long getBuildNumberFromQueue(long queueId);

    record BuildInfo(BuildStatus status, long buildNumber, String url) {
    }

    BuildInfo getBuild(String jobName, long buildNumber);

    String getConsoleText(String jobName, long buildNumber);

    void stopBuild(String jobName, long buildNumber);
}
