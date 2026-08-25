package com.company.release.alert.domain;

/**
 * 报警普通重复通知决策器（ADR-007）。升级通知由 EscalationDecider 独立决策。
 */
public class AlertNotifyDecider {

    private final AlertNotificationPolicy policy;

    public AlertNotifyDecider(AlertNotificationPolicy policy) {
        this.policy = policy;
    }

    /**
     * @param notifiedRepeatCount      已发送的普通重复通知次数（首次=0）
     * @param minutesSinceLastNotify   距上次普通通知的分钟数
     * @param acknowledged             是否已 ACK；ACK 后停止普通重复通知
     */
    public boolean shouldSendRepeatNotification(int notifiedRepeatCount,
                                                long minutesSinceLastNotify,
                                                boolean acknowledged) {
        if (acknowledged) {
            return false;
        }
        if (notifiedRepeatCount == 0) {
            return true; // 首次立即通知
        }
        if (notifiedRepeatCount > policy.maxRepeatCount()) {
            return false;
        }
        return notifiedRepeatCount < policy.maxRepeatCount()
                && minutesSinceLastNotify >= policy.repeatIntervalMinutes();
    }
}
