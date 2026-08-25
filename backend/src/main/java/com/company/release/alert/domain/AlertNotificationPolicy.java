package com.company.release.alert.domain;

/**
 * 报警重复通知策略（规范 §48）：
 * 首次立即通知；之后每隔 repeatIntervalMinutes 通知一次；最多 maxRepeatCount 次普通重复通知。
 */
public record AlertNotificationPolicy(int repeatIntervalMinutes, int maxRepeatCount) {

    public AlertNotificationPolicy {
        if (repeatIntervalMinutes < 0 || maxRepeatCount < 0) {
            throw new IllegalArgumentException("repeatIntervalMinutes/maxRepeatCount must be >= 0");
        }
    }
}
