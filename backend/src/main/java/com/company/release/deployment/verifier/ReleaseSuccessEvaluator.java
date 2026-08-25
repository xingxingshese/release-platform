package com.company.release.deployment.verifier;

import java.util.List;

/**
 * 发布最终成功判定（规范 §66 ReleaseSuccessEvaluator）。
 *
 * SUCCESS = Jenkins AND Deployment AND (可选 Health) AND (可选 Version)
 */
@org.springframework.stereotype.Component
public class ReleaseSuccessEvaluator {

    public record EvaluationResult(boolean success, List<String> failedChecks) {
    }

    public EvaluationResult evaluate(boolean jenkinsSuccess,
                                     boolean deploymentSuccess,
                                     boolean needHealthCheck, boolean healthCheckSuccess,
                                     boolean needVersionCheck, boolean versionCheckSuccess) {
        var failed = new java.util.ArrayList<String>();
        if (!jenkinsSuccess) {
            failed.add("JENKINS");
        }
        if (!deploymentSuccess) {
            failed.add("DEPLOYMENT");
        }
        if (needHealthCheck && !healthCheckSuccess) {
            failed.add("HEALTH_CHECK");
        }
        if (needVersionCheck && !versionCheckSuccess) {
            failed.add("VERSION_CHECK");
        }
        return new EvaluationResult(failed.isEmpty(), List.copyOf(failed));
    }
}
