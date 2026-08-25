package com.company.release.notification;

import com.company.release.alert.notification.NotificationProvider;
import com.company.release.common.observability.ReleaseMetrics;
import com.company.release.notification.routing.NotificationDispatcher;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Phase 15（spec 013）：路由匹配 + 失败渠道不阻断 + 发送留痕与指标。
 */
class NotificationDispatcherTest {

    static class FakeProvider implements NotificationProvider {
        final String channel;
        boolean fail = false;
        int calls = 0;

        FakeProvider(String channel) {
            this.channel = channel;
        }

        @Override
        public String channel() {
            return channel;
        }

        @Override
        public void send(String title, String content, String receiver) {
            calls++;
            if (fail) {
                throw new IllegalStateException("boom");
            }
        }
    }

    private NotificationRuleRepository ruleRepository;
    private NotificationRecordRepository recordRepository;
    private ReleaseMetrics metrics;
    private SimpleMeterRegistry registry;
    private FakeProvider wecom;
    private FakeProvider feishu;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ruleRepository = mock(NotificationRuleRepository.class);
        recordRepository = mock(NotificationRecordRepository.class);
        registry = new SimpleMeterRegistry();
        metrics = new ReleaseMetrics(registry);
        wecom = new FakeProvider("WECOM");
        feishu = new FakeProvider("FEISHU");
    }

    private NotificationDispatcher dispatcher(NotificationRuleEntity... rules) {
        when(ruleRepository.findByEventTypeInAndEnabled(any(), org.mockito.ArgumentMatchers.eq(true)))
                .thenReturn(List.of(rules));
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        return new NotificationDispatcher(ruleRepository, recordRepository, metrics,
                List.of(wecom, feishu));
    }

    private NotificationRuleEntity rule(String event, Long project, String env,
                                        String minLevel, String channel) {
        var r = new NotificationRuleEntity();
        r.setName("r-" + channel);
        r.setEventType(event);
        r.setProjectId(project);
        r.setEnvironmentCode(env);
        r.setMinLevel(minLevel);
        r.setChannelCode(channel);
        r.setReceiverTemplate("https://hook/" + channel.toLowerCase());
        r.setEnabled(true);
        return r;
    }

    @Test
    void routesToMatchingChannelOnly() {
        var d = dispatcher(rule("RELEASE_SUCCESS", 1L, "PROD", "INFO", "WECOM"));
        // 项目/环境匹配 → 发送
        d.dispatch("RELEASE_SUCCESS", 1L, "PROD", "INFO", "t", "c", "release", "1");
        assertThat(wecom.calls).isEqualTo(1);
        assertThat(feishu.calls).isZero();

        // 项目不同 → 不发送
        wecom.calls = 0;
        d.dispatch("RELEASE_SUCCESS", 2L, "PROD", "INFO", "t", "c", "release", "1");
        assertThat(wecom.calls).isZero();
    }

    @Test
    void wildcardAndNullDimensionsMatchEverything() {
        var d = dispatcher(rule("*", null, null, "WARN", "FEISHU"));
        d.dispatch("ALERT_ESCALATED", 9L, "TEST", "CRITICAL", "t", "c", "alert", "7");
        assertThat(feishu.calls).isEqualTo(1);

        // 级别低于 min_level(WARN) → 不发
        feishu.calls = 0;
        d.dispatch("ALERT_ESCALATED", 9L, "TEST", "INFO", "t", "c", "alert", "7");
        assertThat(feishu.calls).isZero();
    }

    @Test
    void failingChannelDoesNotAffectOthersNorThrow() {
        wecom.fail = true;
        var d = dispatcher(rule("ALERT", null, null, "INFO", "WECOM"),
                rule("ALERT", null, null, "INFO", "FEISHU"));

        d.sendVia("WECOM", "hook-a", "t", "c", "alert", "1");   // 失败：记录 success=0
        d.sendVia("FEISHU", "hook-b", "t", "c", "alert", "1");  // 成功

        assertThat(wecom.calls).isEqualTo(1);
        assertThat(feishu.calls).isEqualTo(1);
        assertThat(registry.counter("notification_failed_total").count()).isEqualTo(1.0);
        assertThat(registry.counter("notification_success_total").count()).isEqualTo(1.0);
    }

    @Test
    void unknownChannelRecordedAsFailure() {
        var d = dispatcher();
        registry.clear();
        metrics = new ReleaseMetrics(registry);
        d = dispatcher();
        d.sendVia("NO_SUCH", "x", "t", "c", "release", "1");
        assertThat(registry.counter("notification_failed_total").count()).isEqualTo(1.0);
    }
}
