package com.company.release.release.domain.state;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ADR-003：发布状态机全量转换矩阵测试。
 */
class ReleaseStateMachineTest {

    @Test
    void draftCanOnlyGoToReadyOrCancelled() {
        assertThat(ReleaseStateMachine.allowedNextStates(ReleaseStatus.DRAFT))
                .containsExactlyInAnyOrder(ReleaseStatus.READY, ReleaseStatus.CANCELLED);
    }

    @Test
    void happyPathIsFullyAllowed() {
        ReleaseStatus[] path = {
                ReleaseStatus.DRAFT, ReleaseStatus.READY,
                ReleaseStatus.TEST_MERGING, ReleaseStatus.TEST_DEPLOYING,
                ReleaseStatus.TEST_DEPLOY_SUCCESS, ReleaseStatus.WAIT_TEST_ACCEPT,
                ReleaseStatus.TEST_ACCEPTED, ReleaseStatus.RELEASE_BRANCH_CREATING,
                ReleaseStatus.RELEASE_BRANCH_CREATED,
                ReleaseStatus.PRE_DEPLOYING, ReleaseStatus.PRE_DEPLOY_SUCCESS,
                ReleaseStatus.PROD_DEPLOYING, ReleaseStatus.PROD_DEPLOY_SUCCESS,
                ReleaseStatus.WAIT_PROD_CONFIRM, ReleaseStatus.COMPLETED
        };
        for (int i = 0; i < path.length - 1; i++) {
            assertThat(ReleaseStateMachine.canTransit(path[i], path[i + 1]))
                    .as("%s -> %s", path[i], path[i + 1])
                    .isTrue();
        }
    }

    @Test
    void conflictPathLoopsBackToMerging() {
        assertThat(ReleaseStateMachine.canTransit(ReleaseStatus.TEST_MERGING, ReleaseStatus.WAIT_CONFLICT_RESOLVE)).isTrue();
        assertThat(ReleaseStateMachine.canTransit(ReleaseStatus.WAIT_CONFLICT_RESOLVE, ReleaseStatus.TEST_MERGING)).isTrue();
    }

    @Test
    void cannotBypassConflict() {
        assertThatThrownBy(() -> ReleaseStateMachine.transit(ReleaseStatus.TEST_MERGING, ReleaseStatus.WAIT_TEST_ACCEPT))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void onlyAcceptedPlanCanCreateReleaseBranch() {
        assertThatThrownBy(() -> ReleaseStateMachine.transit(ReleaseStatus.WAIT_TEST_ACCEPT, ReleaseStatus.RELEASE_BRANCH_CREATING))
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThat(ReleaseStateMachine.canTransit(ReleaseStatus.TEST_ACCEPTED, ReleaseStatus.RELEASE_BRANCH_CREATING)).isTrue();
    }

    @Test
    void prodConfirmRequiredBeforeCompleted() {
        // PROD_DEPLOY_SUCCESS 必须先进入 WAIT_PROD_CONFIRM
        assertThatThrownBy(() -> ReleaseStateMachine.transit(ReleaseStatus.PROD_DEPLOY_SUCCESS, ReleaseStatus.COMPLETED))
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThat(ReleaseStateMachine.canTransit(ReleaseStatus.WAIT_PROD_CONFIRM, ReleaseStatus.COMPLETED)).isTrue();
    }

    @Test
    void failureStatesReachableFromActiveStates() {
        assertThat(ReleaseStateMachine.canTransit(ReleaseStatus.TEST_DEPLOYING, ReleaseStatus.FAILED)).isTrue();
        assertThat(ReleaseStateMachine.canTransit(ReleaseStatus.TEST_DEPLOYING, ReleaseStatus.TIMEOUT)).isTrue();
        assertThat(ReleaseStateMachine.canTransit(ReleaseStatus.PROD_DEPLOYING, ReleaseStatus.FAILED)).isTrue();
        assertThat(ReleaseStateMachine.canTransit(ReleaseStatus.READY, ReleaseStatus.CANCELLED)).isTrue();
    }

    @Test
    void terminalStatesHaveNoNext() {
        for (ReleaseStatus s : Set.of(ReleaseStatus.COMPLETED, ReleaseStatus.FAILED,
                ReleaseStatus.TIMEOUT, ReleaseStatus.CANCELLED)) {
            assertThat(ReleaseStateMachine.isTerminal(s)).isTrue();
            assertThat(ReleaseStateMachine.allowedNextStates(s)).isEmpty();
            assertThatThrownBy(() -> ReleaseStateMachine.transit(s, ReleaseStatus.READY))
                    .isInstanceOf(IllegalStateTransitionException.class);
        }
    }
}
