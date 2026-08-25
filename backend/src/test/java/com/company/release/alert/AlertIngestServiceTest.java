package com.company.release.alert;

import com.company.release.alert.application.AlertIngestService;
import com.company.release.alert.domain.AlertEntity;
import com.company.release.alert.domain.AlertFingerprintBuilder;
import com.company.release.alert.domain.AlertNotificationPolicy;
import com.company.release.alert.domain.AlertNotifyDecider;
import com.company.release.alert.notification.NotificationProvider;
import com.company.release.alert.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ADR-007 用例基线：首次立即通知、同指纹合并、ACK 停普通通知、恢复置 RESOLVED。
 */
class AlertIngestServiceTest {

    static class FakeNotificationProvider implements NotificationProvider {
        final List<String> sent = new ArrayList<>();

        @Override
        public String channel() {
            return "INTERNAL";
        }

        @Override
        public void send(String title, String content, String receiver) {
            sent.add(title);
        }
    }

    private AlertRepository alertRepository;
    private FakeNotificationProvider notifier;
    private AlertIngestService service;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        alertRepository = mock(AlertRepository.class);
        notifier = new FakeNotificationProvider();
        now = LocalDateTime.now();
        // 时间函数可注入，模拟时间流逝
        service = new AlertIngestService(alertRepository,
                new AlertFingerprintBuilder(),
                List.of(notifier),
                () -> now,
                5);
        when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private AlertIngestService.InboundAlert inbound(String env) {
        return new AlertIngestService.InboundAlert(
                "order", "接口错误率过高", "ERROR rate > 5%", "CRITICAL",
                env == null ? "prod" : env, "order-service", "db=primary");
    }

    private Optional<AlertEntity> noOpen() {
        return Optional.empty();
    }

    @Test
    void firstEventCreatesAlertAndNotifiesOnce() {
        when(alertRepository.findFirstByProjectIdAndFingerprintAndStatusInOrderByLastOccurredAtDesc(
                any(), any(), any())).thenReturn(noOpen());

        var alert = service.ingest(inbound(null));

        assertThat(alert.getStatus()).isEqualTo("ALERTING");
        assertThat(alert.getLevel()).isEqualTo("CRITICAL");
        assertThat(notifier.sent).hasSize(1); // 首次立即通知
    }

    @Test
    void sameFingerprintMergesIntoOneAlert() {
        var existing = existingOpenAlert();
        when(alertRepository.findFirstByProjectIdAndFingerprintAndStatusInOrderByLastOccurredAtDesc(
                any(), any(), any())).thenReturn(Optional.of(existing));

        // 模拟 100 次重放（间隔 < 重复通知窗口）
        for (int i = 0; i < 100; i++) {
            now = now.plusSeconds(1); // 每秒一次
            service.ingest(inbound(null));
        }

        assertThat(existing.getNotifiedRepeatCount()).isEqualTo(0); // 间隔 5 分钟内不重复通知
        assertThat(existing.getLastOccurredAt()).isEqualTo(now);    // 仅更新 last_occurred_at
        assertThat(notifier.sent).hasSize(0);                       // 首条已在此前发过；本测试从已有报警开始
    }

    @Test
    void repeatAfterIntervalNotifiesAgain() {
        var existing = existingOpenAlert();
        when(alertRepository.findFirstByProjectIdAndFingerprintAndStatusInOrderByLastOccurredAtDesc(
                any(), any(), any())).thenReturn(Optional.of(existing));

        now = now.plusMinutes(6); // 超过默认 5 分钟间隔
        service.ingest(inbound(null));

        assertThat(existing.getNotifiedRepeatCount()).isEqualTo(1);
        assertThat(notifier.sent).hasSize(1);
    }

    @Test
    void acknowledgeStopsNormalNotifications() {
        var existing = existingOpenAlert();
        existing.setAcknowledgedBy(9L);
        existing.setAcknowledgedAt(now);
        when(alertRepository.findFirstByProjectIdAndFingerprintAndStatusInOrderByLastOccurredAtDesc(
                any(), any(), any())).thenReturn(Optional.of(existing));

        now = now.plusMinutes(10);
        service.ingest(inbound(null));

        assertThat(existing.getNotifiedRepeatCount()).isEqualTo(0);
        assertThat(notifier.sent).isEmpty(); // ACK 后普通重复通知停止
    }

    @Test
    void resolveMarksResolvedWithTimestamp() {
        var existing = existingOpenAlert();
        when(alertRepository.findById(77L)).thenReturn(Optional.of(existing));
        when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.resolve(7L, 77L);

        assertThat(existing.getStatus()).isEqualTo("RESOLVED");
        assertThat(existing.getResolvedAt()).isNotNull();
    }

    private AlertEntity existingOpenAlert() {
        var a = new AlertEntity();
        a.setId(77L);
        a.setProjectId(1L);
        a.setTitle("接口错误率过高");
        a.setLevel("CRITICAL");
        a.setStatus("ALERTING");
        a.setFirstOccurredAt(now.minusMinutes(2));
        a.setLastOccurredAt(now);
        a.setFingerprint(new AlertFingerprintBuilder()
                .build("order", "order-service", "prod", "", "db=primary"));
        return a;
    }
}
