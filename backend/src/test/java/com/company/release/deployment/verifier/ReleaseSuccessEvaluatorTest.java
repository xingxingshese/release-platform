package com.company.release.deployment.verifier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 规范 §66：Jenkins SUCCESS ≠ 发布成功。
 * Jenkins + Deployment + (可选) Health + (可选) Version 全部通过才算 SUCCESS。
 */
class ReleaseSuccessEvaluatorTest {

    private final ReleaseSuccessEvaluator evaluator = new ReleaseSuccessEvaluator();

    @Test
    void allChecksPassIsSuccess() {
        var r = evaluator.evaluate(true, true, true, true, true, true);
        assertThat(r.success()).isTrue();
    }

    @Test
    void jenkinsSuccessAloneIsNotEnough() {
        // 红线用例：只有 Jenkins SUCCESS，Deployment 未验证
        var r = evaluator.evaluate(true, false, true, true, true, true);
        assertThat(r.success()).isFalse();
        assertThat(r.failedChecks()).containsExactly("DEPLOYMENT");
    }

    @Test
    void healthCheckFailurePreventsSuccessWhenRequired() {
        var r = evaluator.evaluate(true, true, true, false, true, true);
        assertThat(r.success()).isFalse();
        assertThat(r.failedChecks()).containsExactly("HEALTH_CHECK");
    }

    @Test
    void versionMismatchPreventsSuccessWhenRequired() {
        var r = evaluator.evaluate(true, true, true, true, true, false);
        assertThat(r.success()).isFalse();
        assertThat(r.failedChecks()).containsExactly("VERSION_CHECK");
    }

    @Test
    void disabledChecksDoNotBlockSuccess() {
        var r = evaluator.evaluate(true, true, false, false, false, false);
        assertThat(r.success()).isTrue();
        assertThat(r.failedChecks()).isEmpty();
    }

    @Test
    void collectsAllFailures() {
        var r = evaluator.evaluate(false, false, true, false, true, false);
        assertThat(r.failedChecks())
                .containsExactlyInAnyOrder("JENKINS", "DEPLOYMENT", "HEALTH_CHECK", "VERSION_CHECK");
    }
}
