package com.company.release.alert.domain;

/**
 * 报警升级级别配置（规范 §50 AlertEscalation）。
 */
public record EscalationLevel(int level, int delayMinutes, String receiverRole) {
}
