package com.company.release.release.domain.state;

/**
 * 非法状态转换（ADR-003）。
 */
public class IllegalStateTransitionException extends RuntimeException {

    private final ReleaseStatus from;
    private final ReleaseStatus to;

    public IllegalStateTransitionException(ReleaseStatus from, ReleaseStatus to) {
        super("Illegal release state transition: " + from + " -> " + to);
        this.from = from;
        this.to = to;
    }

    public ReleaseStatus getFrom() {
        return from;
    }

    public ReleaseStatus getTo() {
        return to;
    }
}
