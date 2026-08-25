package com.company.release.deployment.verifier;

import com.company.release.deployment.domain.KubernetesDeploymentSnapshot;
import com.company.release.deployment.domain.PodState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ADR-006 TDD 用例基线：
 * 1. 4/4 Ready → SUCCESS
 * 2. 3/4 Ready → RUNNING
 * 3. desired=ready=4 但 unavailable=1 → FAILED
 * 4. CrashLoopBackOff → FAILED
 * 5. 超时 → TIMEOUT
 */
class KubernetesDeploymentVerifierTest {

    private final KubernetesDeploymentVerifier verifier = new KubernetesDeploymentVerifier();

    @Test
    void case1_allReplicasReadyIsSuccess() {
        var snapshot = snapshot(4, 4, 4, 4, 0,
                List.of(pod("READY"), pod("READY"), pod("READY"), pod("READY")), false);
        assertThat(verifier.verify(snapshot)).isEqualTo(VerifyResult.SUCCESS);
    }

    @Test
    void case2_partialRolloutIsRunning() {
        var snapshot = snapshot(4, 2, 3, 3, 1,
                List.of(pod("READY"), pod("READY"), pod("READY"), pod("PENDING")), false);
        assertThat(verifier.verify(snapshot)).isEqualTo(VerifyResult.RUNNING);
    }

    @Test
    void case3_unavailableReplicaIsFailed() {
        var snapshot = snapshot(4, 4, 4, 3, 1,
                List.of(pod("READY"), pod("READY"), pod("READY"), pod("STARTING")), false);
        assertThat(verifier.verify(snapshot)).isEqualTo(VerifyResult.FAILED);
    }

    @Test
    void case4_crashLoopBackOffFailsWholeDeployment() {
        var snapshot = snapshot(4, 4, 3, 3, 1,
                List.of(pod("READY"), pod("READY"), pod("READY"),
                        new PodState("FAILED", "CrashLoopBackOff")), false);
        assertThat(verifier.verify(snapshot)).isEqualTo(VerifyResult.FAILED);
    }

    @Test
    void case5_timeoutIsTimeout() {
        var snapshot = snapshot(4, 2, 2, 2, 2,
                List.of(pod("READY"), pod("READY"), pod("PENDING"), pod("PENDING")), true);
        assertThat(verifier.verify(snapshot)).isEqualTo(VerifyResult.TIMEOUT);
    }

    @Test
    void imagePullBackOffAlsoFails() {
        var snapshot = snapshot(1, 0, 0, 0, 1,
                List.of(new PodState("FAILED", "ImagePullBackOff")), false);
        assertThat(verifier.verify(snapshot)).isEqualTo(VerifyResult.FAILED);
    }

    private KubernetesDeploymentSnapshot snapshot(int desired, int updated, int ready,
                                                  int available, int unavailable,
                                                  List<PodState> pods, boolean timedOut) {
        return new KubernetesDeploymentSnapshot(desired, updated, ready, available, unavailable, pods, timedOut);
    }

    private PodState pod(String status) {
        return new PodState(status, null);
    }
}
