package com.company.release.common.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 规范 §二十七：业务指标存在性。 */
class ReleaseMetricsTest {

    @Test
    void countersIncrement() {
        var registry = new SimpleMeterRegistry();
        var m = new ReleaseMetrics(registry);

        m.releaseStarted();
        m.releaseFinished(true);
        m.releaseFinished(false);
        m.alertIngested();
        m.alertEscalated();
        m.notificationSent(true);
        m.notificationSent(false);

        assertThat(m.count("release_total")).isEqualTo(1.0);
        assertThat(m.count("release_success_total")).isEqualTo(1.0);
        assertThat(m.count("release_failed_total")).isEqualTo(1.0);
        assertThat(m.count("alert_total")).isEqualTo(1.0);
        assertThat(m.count("alert_escalation_total")).isEqualTo(1.0);
        assertThat(m.count("notification_success_total")).isEqualTo(1.0);
        assertThat(m.count("notification_failed_total")).isEqualTo(1.0);
    }

    @Test
    void missingCounterIsZeroNotException() {
        var m = new ReleaseMetrics(new SimpleMeterRegistry());
        assertThat(m.count("never_used")).isZero();
    }
}
