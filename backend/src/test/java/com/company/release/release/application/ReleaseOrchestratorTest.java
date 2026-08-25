package com.company.release.release.application;

import com.company.release.common.exception.BusinessException;
import com.company.release.deployment.verifier.KubernetesDeploymentVerifier;
import com.company.release.deployment.verifier.ReleaseSuccessEvaluator;
import com.company.release.git.FakeGitProvider;
import com.company.release.git.application.GitMergeService;
import com.company.release.jenkins.FakeJenkinsProvider;
import com.company.release.jenkins.api.JenkinsProvider;
import com.company.release.release.domain.model.PlanServiceEntity;
import com.company.release.release.domain.model.ReleasePlanEntity;
import com.company.release.release.domain.model.ReleaseTaskEntity;
import com.company.release.release.repository.PlanServiceRepository;
import com.company.release.release.repository.ReleasePlanRepository;
import com.company.release.release.repository.ReleaseTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Phase 8：测试发布编排（规范 §14）。
 * 校验 → merge release_test → 冲突暂停 → Jenkins → 成功判定（红线：Jenkins SUCCESS ≠ 发布成功）。
 */
class ReleaseOrchestratorTest {

    private FakeGitProvider fakeGit;
    private FakeJenkinsProvider fakeJenkins;
    private ReleasePlanRepository planRepository;
    private ReleaseTaskRepository taskRepository;
    private PlanServiceRepository planServiceRepository;
    private ReleaseOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        fakeGit = new FakeGitProvider();
        fakeJenkins = new FakeJenkinsProvider();
        planRepository = mock(ReleasePlanRepository.class);
        taskRepository = mock(ReleaseTaskRepository.class);
        planServiceRepository = mock(PlanServiceRepository.class);

        orchestrator = new ReleaseOrchestrator(
                planRepository, taskRepository, planServiceRepository,
                new GitMergeService(Map.of("FAKE", fakeGit)),
                fakeJenkins,
                new ReleaseSuccessEvaluator(),
                new KubernetesDeploymentVerifier());
    }

    private ReleasePlanEntity readyPlan() {
        var p = new ReleasePlanEntity();
        p.setId(1L);
        p.setProjectId(10L);
        p.setStatus("READY");
        p.setEnvironments("TEST");
        return p;
    }

    private PlanServiceEntity branch() {
        var b = new PlanServiceEntity();
        b.setId(9L);
        b.setReleasePlanId(1L);
        b.setServiceId(3L);
        b.setSourceBranch("feature/order-123");
        b.setTargetTestBranch("release_test");
        return b;
    }

    private void stubPlanAndBranches(ReleasePlanEntity plan) {
        when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
        when(taskRepository.findByReleasePlanIdAndEnvironmentCode(plan.getId(), "TEST"))
                .thenReturn(Optional.empty());
        when(planServiceRepository.findByReleasePlanId(plan.getId())).thenReturn(List.of(branch()));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void happyPathMergesThenTriggersJenkinsBuild() {
        var plan = readyPlan();
        stubPlanAndBranches(plan);
        var task = orchestrator.startTestRelease(plan.getId());

        assertThat(fakeGit.mergeCalls).isEqualTo(1);              // 平台执行 merge（规范 §15）
        assertThat(fakeJenkins.triggered).hasSize(1);             // 无冲突 → Jenkins
        assertThat(fakeJenkins.triggered.get(0).params().get("BRANCH")).isEqualTo("feature/order-123"); // 参数映射
        assertThat(task.getStatus()).isEqualTo("RUNNING");
        assertThat(task.getJenkinsQueueId()).isNotNull();
    }

    @Test
    void conflictSuspendsToWaitConflictResolveWithoutJenkins() {
        var plan = readyPlan();
        stubPlanAndBranches(plan);
        fakeGit.conflict = true;

        assertThatThrownBy(() -> orchestrator.startTestRelease(plan.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("conflict");

        assertThat(fakeJenkins.triggered).isEmpty();               // 冲突绝不进入 Jenkins
        assertThat(plan.getStatus()).isEqualTo("WAIT_CONFLICT_RESOLVE"); // 规范 §44：禁止自动绕过冲突
    }

    @Test
    void buildFinishedAllChecksPassMakesTaskSuccess() {
        var task = runningTask();
        when(taskRepository.findByJenkinsServerIdAndJenkinsJobNameAndJenkinsBuildNumber(
                1L, "order-service-test", 582L)).thenReturn(Optional.of(task));

        orchestrator.onBuildFinished(1L, "order-service-test", 582L, "SUCCESS",
                true, true, true, true, true);

        assertThat(task.getStatus()).isEqualTo("SUCCESS");
        assertThat(task.getFinishedAt()).isNotNull();
    }

    @Test
    void jenkinsSuccessAloneDoesNotMakeTaskSuccess() {
        // 核心红线用例：Deployment 未通过 → FAILED
        var task = runningTask();
        when(taskRepository.findByJenkinsServerIdAndJenkinsJobNameAndJenkinsBuildNumber(
                1L, "order-service-test", 582L)).thenReturn(Optional.of(task));

        orchestrator.onBuildFinished(1L, "order-service-test", 582L, "SUCCESS",
                false, false, false, true, true);

        assertThat(task.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void buildFailureMarksTaskFailed() {
        var task = runningTask();
        when(taskRepository.findByJenkinsServerIdAndJenkinsJobNameAndJenkinsBuildNumber(
                1L, "order-service-test", 582L)).thenReturn(Optional.of(task));

        orchestrator.onBuildFinished(1L, "order-service-test", 582L, "FAILURE",
                false, false, false, true, true);

        assertThat(task.getStatus()).isEqualTo("FAILED");
    }

    private ReleaseTaskEntity runningTask() {
        var t = new ReleaseTaskEntity();
        t.setId(55L);
        t.setReleasePlanId(1L);
        t.setEnvironmentCode("TEST");
        t.setStatus("RUNNING");
        t.setJenkinsBuildNumber(582L);
        return t;
    }
}
