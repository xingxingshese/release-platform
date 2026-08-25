package com.company.release.release.domain.state;

import java.util.Map;
import java.util.Set;

import static com.company.release.release.domain.state.ReleaseStatus.*;

/**
 * 发布状态机（ADR-003）。
 * 集中维护全部合法转换；业务代码禁止散落 if/else 改状态。
 */
public final class ReleaseStateMachine {

    private static final Map<ReleaseStatus, Set<ReleaseStatus>> TRANSITIONS = Map.ofEntries(
            Map.entry(DRAFT, Set.of(READY, CANCELLED)),

            // READY 可直接进入 PRE/PROD（支持"只发预发 / 只发生产"，规范 §18）
            Map.entry(READY, Set.of(TEST_MERGING, PRE_DEPLOYING, PROD_DEPLOYING, CANCELLED)),

            Map.entry(TEST_MERGING, Set.of(WAIT_CONFLICT_RESOLVE, TEST_DEPLOYING, FAILED)),
            Map.entry(WAIT_CONFLICT_RESOLVE, Set.of(TEST_MERGING, CANCELLED)),
            Map.entry(TEST_DEPLOYING, Set.of(TEST_DEPLOY_SUCCESS, FAILED, TIMEOUT)),
            Map.entry(TEST_DEPLOY_SUCCESS, Set.of(WAIT_TEST_ACCEPT)),

            Map.entry(WAIT_TEST_ACCEPT, Set.of(TEST_ACCEPTED, TEST_REJECTED, TIMEOUT)),
            Map.entry(TEST_REJECTED, Set.of(READY, CANCELLED)),
            Map.entry(TEST_ACCEPTED, Set.of(RELEASE_BRANCH_CREATING)),

            Map.entry(RELEASE_BRANCH_CREATING, Set.of(RELEASE_BRANCH_CREATED, FAILED)),
            Map.entry(RELEASE_BRANCH_CREATED, Set.of(PRE_DEPLOYING, PROD_DEPLOYING, CANCELLED)),

            Map.entry(PRE_DEPLOYING, Set.of(PRE_DEPLOY_SUCCESS, FAILED, TIMEOUT)),
            Map.entry(PRE_DEPLOY_SUCCESS, Set.of(PROD_DEPLOYING)),

            Map.entry(PROD_DEPLOYING, Set.of(PROD_DEPLOY_SUCCESS, FAILED, TIMEOUT)),
            Map.entry(PROD_DEPLOY_SUCCESS, Set.of(WAIT_PROD_CONFIRM)),
            Map.entry(WAIT_PROD_CONFIRM, Set.of(COMPLETED, TIMEOUT))
    );

    private static final Set<ReleaseStatus> TERMINAL =
            Set.of(COMPLETED, FAILED, TIMEOUT, CANCELLED);

    private ReleaseStateMachine() {
    }

    public static boolean canTransit(ReleaseStatus from, ReleaseStatus to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public static void transit(ReleaseStatus from, ReleaseStatus to) {
        if (!canTransit(from, to)) {
            throw new IllegalStateTransitionException(from, to);
        }
    }

    public static Set<ReleaseStatus> allowedNextStates(ReleaseStatus from) {
        return TRANSITIONS.getOrDefault(from, Set.of());
    }

    public static boolean isTerminal(ReleaseStatus status) {
        return TERMINAL.contains(status);
    }
}
