package com.company.release.common.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * 业务指标（规范 §二十七）：release/alert/notification 指标全集。
 * Prometheus 端点经 actuator 暴露：/actuator/prometheus。
 */
@Component
public class ReleaseMetrics {

    private final MeterRegistry registry;

    public ReleaseMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void releaseStarted() {
        counter("release_total").increment();
    }

    public void releaseFinished(boolean success) {
        if (success) {
            counter("release_success_total").increment();
        } else {
            counter("release_failed_total").increment();
        }
    }

    public void alertIngested() {
        counter("alert_total").increment();
    }

    public void alertEscalated() {
        counter("alert_escalation_total").increment();
    }

    public void notificationSent(boolean success) {
        (success ? counter("notification_success_total") : counter("notification_failed_total")).increment();
    }

    public double count(String name) {
        return registry.find(name).counter() == null ? 0.0 : registry.find(name).counter().count();
    }

    Counter counter(String name) {
        return Counter.builder(name).register(registry);
    }
}
