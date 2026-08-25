package com.company.release.alert.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 报警升级决策器（ADR-007）。
 * 规则：按 elapsedMinutes 命中已到期的最高级别；ACK 不阻止升级；RESOLVED 终止升级。
 */
public class EscalationDecider {

    private final List<EscalationLevel> levels;

    public EscalationDecider(List<EscalationLevel> levels) {
        this.levels = levels.stream()
                .sorted(Comparator.comparingInt(EscalationLevel::delayMinutes))
                .toList();
    }

    /** 当前应处于的升级级别（未配置 0 分钟级别时可能为空）。 */
    public Optional<Integer> currentLevel(long elapsedMinutes) {
        return levels.stream()
                .filter(l -> elapsedMinutes >= l.delayMinutes())
                .max(Comparator.comparingInt(EscalationLevel::delayMinutes))
                .map(EscalationLevel::level);
    }

    /**
     * 下一次应触发的升级。
     *
     * @param elapsedMinutes       报警持续时间
     * @param escalatedToLevel     已升级到的最大级别（0 = 尚未升级）
     * @param acknowledged         是否已 ACK（ACK 不阻止升级）
     */
    public Optional<Integer> nextLevel(long elapsedMinutes, int escalatedToLevel, boolean acknowledged) {
        return nextLevel(elapsedMinutes, escalatedToLevel, acknowledged, false);
    }

    /**
     * @param resolved 是否已恢复；RESOLVED 后不再升级。
     */
    public Optional<Integer> nextLevel(long elapsedMinutes, int escalatedToLevel,
                                       boolean acknowledged, boolean resolved) {
        if (resolved) {
            return Optional.empty();
        }
        // acknowledged 参数保留语义：ACK 只停普通通知，不停升级（ADR-007 红线）
        // 初始通知已覆盖最低级别：起始级别下限 = max(已升级级别, 最低级别)
        int lowest = levels.isEmpty() ? 0 : levels.get(0).level();
        int effectiveCurrent = Math.max(escalatedToLevel, lowest);
        return levels.stream()
                .filter(l -> l.level() > effectiveCurrent)
                .filter(l -> elapsedMinutes >= l.delayMinutes())
                .max(Comparator.comparingInt(EscalationLevel::level))
                .map(EscalationLevel::level);
    }
}
