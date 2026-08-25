package com.company.release.alert.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ADR-007 频率控制测试：
 * - 首次立即通知
 * - 重复通知按间隔
 * - 超过最大次数停止普通通知（即使未 ACK）
 * - ACK 后普通重复通知停止，但不影响升级（升级由 EscalationDecider 负责）
 */
class AlertNotifyDeciderTest {

    private final AlertNotificationPolicy policy = new AlertNotificationPolicy(5, 3);

    @Test
    void firstOccurrenceNotifiesImmediately() {
        assertThat(new AlertNotifyDecider(policy)
                .shouldSendRepeatNotification(0, 0, false)).isTrue();
    }

    @Test
    void repeatBeforeIntervalIsSuppressed() {
        // 已通知 1 次，2 分钟后再次发生 → 抑制
        assertThat(new AlertNotifyDecider(policy)
                .shouldSendRepeatNotification(1, 2, false)).isFalse();
    }

    @Test
    void repeatAfterIntervalNotifies() {
        assertThat(new AlertNotifyDecider(policy)
                .shouldSendRepeatNotification(1, 5, false)).isTrue();
    }

    @Test
    void stopsAfterMaxRepeatCount() {
        // 已通知 3 次 = maxRepeatCount → 停止
        assertThat(new AlertNotifyDecider(policy)
                .shouldSendRepeatNotification(3, 100, false)).isFalse();
    }

    @Test
    void ackStopsNormalRepeatNotifications() {
        assertThat(new AlertNotifyDecider(policy)
                .shouldSendRepeatNotification(1, 10, true)).isFalse();
    }
}
