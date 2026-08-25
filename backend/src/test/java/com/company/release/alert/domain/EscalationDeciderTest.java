package com.company.release.alert.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ADR-007 升级规则测试：
 * - 按时间逐级升级
 * - ACK 不阻止升级（红线）
 * - RESOLVED 不再升级
 */
class EscalationDeciderTest {

    private final List<EscalationLevel> levels = List.of(
            new EscalationLevel(1, 0, "DEVELOPER"),
            new EscalationLevel(2, 10, "TECH_LEAD"),
            new EscalationLevel(3, 20, "PROJECT_OWNER"),
            new EscalationLevel(4, 30, "DUTY_MANAGER")
    );

    private final EscalationDecider decider = new EscalationDecider(levels);

    @Test
    void level1AtZeroMinutes() {
        assertThat(decider.currentLevel(0)).contains(1);
        assertThat(decider.nextLevel(0, 0, false)).isEmpty();
    }

    @Test
    void escalatesByElapsedMinutes() {
        assertThat(decider.currentLevel(5)).contains(1);
        assertThat(decider.currentLevel(10)).contains(2);
        assertThat(decider.currentLevel(25)).contains(3);
        assertThat(decider.currentLevel(30)).contains(4);
    }

    @Test
    void ackDoesNotBlockEscalation() {
        // 关键红线：ACK 后 10 分钟仍未恢复 → 仍升级到 Level 2
        Optional<Integer> next = decider.nextLevel(10, 1 /* already escalated to L1 */, true /* acked */);
        assertThat(next).contains(2);
    }

    @Test
    void resolvedStopsEscalation() {
        assertThat(decider.nextLevel(30, 1, false, true)).isEmpty();
    }

    @Test
    void noDoubleEscalationToSameLevel() {
        assertThat(decider.nextLevel(35, 4, false)).isEmpty();
    }
}
