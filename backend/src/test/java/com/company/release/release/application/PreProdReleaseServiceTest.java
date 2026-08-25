package com.company.release.release.application;

import com.company.release.common.exception.BusinessException;
import com.company.release.common.observability.ReleaseMetrics;
import com.company.release.common.redis.DistributedLockService;
import com.company.release.iam.PermissionChecker;
import com.company.release.jenkins.FakeJenkinsProvider;
import com.company.release.release.domain.model.PlanServiceEntity;
import com.company.release.release.domain.model.ReleasePlanEntity;
import com.company.release.release.domain.model.ReleaseTaskEntity;
import com.company.release.release.repository.PlanServiceRepository;
import com.company.release.release.repository.ReleasePlanRepository;
import com.company.release.release.repository.ReleaseTaskRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Phase 11/12：预发/生产编排（spec 009/010）。
 * 权限（release:prod:execute）、前置状态守卫（并行开关配置化）、分布式锁防重复。
 */
class PreProdReleaseServiceTest {

    private ReleasePlanRepository planRepository;
    private ReleaseTaskRepository taskRepository;
    private PlanServiceRepository planServiceRepository;
    private PermissionChecker permissionChecker;
    private FakeJenkinsProvider fakeJenkins;
    private DistributedLockService lockService;
    private PreProdReleaseService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        planRepository = mock(ReleasePlanRepository.class);
        taskRepository = mock(ReleaseTaskRepository.class);
        planServiceRepository = mock(PlanServiceRepository.class);
        permissionChecker = mock(PermissionChecker.class);
        fakeJenkins = new FakeJenkinsProvider();
        lockService = mock(DistributedLockService.class);
        when(lockService.tryLock(anyString(), anyString(), any())).thenReturn(true);

        service = new PreProdReleaseService(planRepository, taskRepository, planServiceRepository,
                fakeJenkins, permissionChecker, lockService,
                new ReleaseMetrics(new SimpleMeterRegistry()), false);
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private ReleasePlanEntity planIn(String status) {
        var p = new ReleasePlanEntity();
        p.setId(1L);
        p.setStatus(status);
        return p;
    }

    private void withReleaseBranch() {
        var s = new PlanServiceEntity();
        s.setReleaseBranch("release_20260825_1");
        when(planServiceRepository.findByReleasePlanId(1L)).thenReturn(List.of(s));
    }

    @Test
    void preRequiresReleaseBranchCreatedState() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(planIn("TEST_ACCEPTED")));
        assertThatThrownBy(() -> service.startPreRelease(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("RELEASE_BRANCH_CREATED");
    }

    @Test
    void preHappyPathTriggersJenkinsWithReleaseBranch() {
        withReleaseBranch();
        when(planRepository.findById(1L)).thenReturn(Optional.of(planIn("RELEASE_BRANCH_CREATED")));

        var task = service.startPreRelease(1L, 100L);

        assertThat(task.getStatus()).isEqualTo("RUNNING");
        assertThat(fakeJenkins.triggered).hasSize(1);
        assertThat(fakeJenkins.triggered.get(0).params().get("BRANCH")).isEqualTo("release_20260825_1");
        assertThat(fakeJenkins.triggered.get(0).params().get("ENV")).isEqualTo("PRE");
    }

    @Test
    void prodBlockedWithoutPreSuccessWhenParallelDisabled() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(planIn("RELEASE_BRANCH_CREATED")));
        // 并行关闭：RELEASE_BRANCH_CREATED 不允许直接 PROD
        assertThatThrownBy(() -> service.startProdRelease(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PROD not allowed");
    }

    @Test
    void prodAllowedFromPreSuccess() {
        withReleaseBranch();
        var plan = planIn("PRE_DEPLOY_SUCCESS");
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));

        var task = service.startProdRelease(1L, 100L);

        assertThat(task.getEnvironmentCode()).isEqualTo("PROD");
        assertThat(plan.getStatus()).isEqualTo("PROD_DEPLOYING");
        assertThat(fakeJenkins.triggered.get(0).params().get("ENV")).isEqualTo("PROD");
    }

    @Test
    void prodParallelModeAllowsDirectlyAfterBranchCreated() {
        service = new PreProdReleaseService(planRepository, taskRepository, planServiceRepository,
                fakeJenkins, permissionChecker, lockService,
                new ReleaseMetrics(new SimpleMeterRegistry()), true);
        withReleaseBranch();
        when(planRepository.findById(1L)).thenReturn(Optional.of(planIn("RELEASE_BRANCH_CREATED")));

        var task = service.startProdRelease(1L, 100L);
        assertThat(task.getEnvironmentCode()).isEqualTo("PROD");
    }

    @Test
    void lockHeldRejectsDuplicateTrigger() {
        withReleaseBranch();
        when(planRepository.findById(1L)).thenReturn(Optional.of(planIn("RELEASE_BRANCH_CREATED")));
        when(lockService.tryLock(anyString(), anyString(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.startPreRelease(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already running");
    }

    @Test
    void missingPermissionIsRejected() {
        org.mockito.Mockito.doThrow(new BusinessException(
                        com.company.release.common.exception.ErrorCode.PERMISSION_DENIED, "denied"))
                .when(permissionChecker).checkPermission(100L, PermissionChecker.PROD_EXECUTE);
        when(planRepository.findById(1L)).thenReturn(Optional.of(planIn("RELEASE_BRANCH_CREATED")));

        assertThatThrownBy(() -> service.startProdRelease(1L, 100L))
                .isInstanceOf(BusinessException.class);
        assertThat(fakeJenkins.triggered).isEmpty();
    }
}
