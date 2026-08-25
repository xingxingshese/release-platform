package com.company.release.deployment.adapter;

import com.company.release.deployment.adapter.DeploymentAdapter.DeploymentTarget;
import com.company.release.deployment.adapter.DeploymentAdapter.VerifyOutcome;
import com.company.release.deployment.domain.KubernetesDeploymentSnapshot;
import com.company.release.deployment.domain.PodState;
import com.company.release.deployment.health.HttpHealthChecker;
import com.company.release.deployment.verifier.VerifyResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Phase 13/14：DeploymentAdapter 统一验证 + release_deployment_node 逐节点落库。
 * ADR-006 基线用例经 FakeKubernetesSnapshotFetcher 驱动（规范 §二十：Fake 外部系统）。
 */
class DeploymentVerificationServiceTest {

    static class FakeKubernetesSnapshotFetcher implements KubernetesSnapshotFetcher {
        KubernetesDeploymentSnapshot next;

        @Override
        public KubernetesDeploymentSnapshot fetch(String namespace, String deploymentName) {
            return next;
        }
    }

    private FakeKubernetesSnapshotFetcher fakeFetcher;
    private ReleaseDeploymentNodeRepository nodeRepository;
    private DeploymentVerificationService service;

    @BeforeEach
    void setUp() {
        fakeFetcher = new FakeKubernetesSnapshotFetcher();
        nodeRepository = mock(ReleaseDeploymentNodeRepository.class);
        service = new DeploymentVerificationService(
                List.of(new KubernetesAdapter(fakeFetcher),
                        new FrontendDeploymentAdapter(
                                url -> new HttpHealthChecker.HttpResponseLike(200, "ok"),
                                url -> "{\"version\":\"v1.2.3\"}")),
                nodeRepository);
    }

    private KubernetesDeploymentSnapshot snapshot(int desired, int ready, int unavailable,
                                                  List<PodState> pods, boolean timedOut) {
        return new KubernetesDeploymentSnapshot(desired, ready, ready, desired - unavailable, unavailable,
                pods, timedOut);
    }

    @Test
    void k8sAllReadyIsSuccessAndRecorded() {
        fakeFetcher.next = snapshot(4, 4, 0,
                List.of(new PodState("READY", null), new PodState("READY", null),
                        new PodState("READY", null), new PodState("READY", null)), false);

        var outcome = service.verifyAndRecord(
                DeploymentTarget.k8s(9L, "order-service", "prod", "order-deploy"), "pod-1");

        assertThat(outcome.result()).isEqualTo(VerifyResult.SUCCESS);
        var captor = org.mockito.ArgumentCaptor.forClass(ReleaseDeploymentNodeEntity.class);
        verify(nodeRepository).save(captor.capture());
        var node = captor.getValue();
        assertThat(node.getReleaseTaskId()).isEqualTo(9L);
        assertThat(node.getResult()).isEqualTo("SUCCESS");
        assertThat(node.getReplicaReady()).isEqualTo(4);
        assertThat(node.getHealthPassed()).isTrue();
    }

    @Test
    void k8sCrashLoopBackOffIsFailed() {
        fakeFetcher.next = snapshot(4, 4, 1,
                List.of(new PodState("READY", null), new PodState("PENDING", "CrashLoopBackOff"),
                        new PodState("READY", null), new PodState("READY", null)), false);

        var outcome = service.verifyAndRecord(
                DeploymentTarget.k8s(9L, "svc", "ns", "deploy"), "pod-x");

        assertThat(outcome.result()).isEqualTo(VerifyResult.FAILED);
    }

    @Test
    void frontendVersionMismatchFails() {
        var adapter = new FrontendDeploymentAdapter(
                url -> new HttpHealthChecker.HttpResponseLike(200, "ok"),
                url -> "{\"version\":\"v0.0.1\"}");
        var outcome = adapter.verify(
                DeploymentTarget.frontend(5L, "web", "https://fe/health", "https://fe/version.json", "v1.2.3"));

        assertThat(outcome.result()).isEqualTo(VerifyResult.VERSION_CHECK_FAILED);
        assertThat(outcome.versionActual()).isEqualTo("v0.0.1");
        assertThat(outcome.versionPassed()).isFalse();
    }

    @Test
    void frontendHealthyPasses() {
        var outcome = service.verifyAndRecord(
                DeploymentTarget.frontend(5L, "web", "https://fe/health", "https://fe/version.json", "v1.2.3"),
                "cdn-edge-1");

        assertThat(outcome.result()).isEqualTo(VerifyResult.SUCCESS);
        assertThat(outcome.versionPassed()).isTrue();
    }
}
