package com.company.release.release.application;

import com.company.release.common.exception.BusinessException;
import com.company.release.common.exception.ConflictException;
import com.company.release.common.redis.DistributedLockService;
import com.company.release.release.domain.model.ReleaseConfigSnapshotEntity;
import com.company.release.release.domain.model.ReleasePlanEntity;
import com.company.release.release.domain.model.ReleaseTaskEntity;
import com.company.release.release.repository.ReleaseConfigSnapshotRepository;
import com.company.release.release.repository.ReleasePlanRepository;
import com.company.release.release.repository.ReleaseTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Phase 5：发布计划核心规则 TDD。
 */
class ReleasePlanServiceTest {

    private ReleasePlanRepository planRepository;
    private ReleaseTaskRepository taskRepository;
    private ReleaseConfigSnapshotRepository snapshotRepository;
    private DistributedLockService lockService;
    private ReleasePlanService service;

    @BeforeEach
    void setUp() {
        planRepository = mock(ReleasePlanRepository.class);
        taskRepository = mock(ReleaseTaskRepository.class);
        snapshotRepository = mock(ReleaseConfigSnapshotRepository.class);
        lockService = mock(DistributedLockService.class);
        service = new ReleasePlanService(planRepository, taskRepository, snapshotRepository, lockService);
    }

    private ReleasePlanEntity draft() {
        var p = new ReleasePlanEntity();
        p.setId(1L);
        p.setProjectId(10L);
        p.setName("8月发布");
        p.setStatus("DRAFT");
        p.setEnvironments("TEST");
        p.setReleaseOwnerId(7L);
        return p;
    }

    @Test
    void createDefaultsToDraftWithCreatorAsOwner() {
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var p = service.create(7L, new ReleasePlanService.CreatePlanCmd(
                10L, "8月发布", "v2026.08", null, null, "TEST"));
        assertThat(p.getStatus()).isEqualTo("DRAFT");
        assertThat(p.getReleaseOwnerId()).isEqualTo(7L);
    }

    @Test
    void readyCreatesConfigSnapshotAndTransits() {
        var plan = draft();
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(snapshotRepository.save(any())).thenAnswer(inv -> {
            var s = (ReleaseConfigSnapshotEntity) inv.getArgument(0);
            s.setId(100L);
            return s;
        });
        when(taskRepository.findByReleasePlanIdAndEnvironmentCode(any(), any()))
                .thenReturn(Optional.empty());
        when(lockService.withLock(anyString(), anyString(), any(), any()))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(3)).get());

        service.ready(7L, 1L);

        assertThat(plan.getStatus()).isEqualTo("READY");
        assertThat(plan.getConfigSnapshotId()).isEqualTo(100L); // ADR-008：快照在启动时生成
    }

    @Test
    void illegalTransitionRejectedByStateMachine() {
        var plan = draft();
        plan.setStatus("TEST_MERGING");
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        assertThatThrownBy(() -> service.transit(7L, plan, "WAIT_TEST_ACCEPT"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Illegal release state transition");
    }

    @Test
    void onlyOwnerOrAdminCanOperate() {
        var plan = draft();
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        // operator != release_owner → 拒绝
        assertThatThrownBy(() -> service.ready(999L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("only release owner");
    }

    @Test
    void duplicateEnvironmentTaskReturnsExisting() {
        var plan = draft();
        plan.setStatus("READY");
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        var existing = new ReleaseTaskEntity();
        existing.setId(55L);
        existing.setReleasePlanId(1L);
        existing.setEnvironmentCode("TEST");
        when(taskRepository.findByReleasePlanIdAndEnvironmentCode(1L, "TEST")).thenReturn(Optional.of(existing));

        var result = service.startEnvironmentRelease(1L, "TEST");
        assertThat(result.getId()).isEqualTo(55L); // 幂等
        verify(taskRepository, never()).save(any());
    }

    @Test
    void environmentNotSelectedInPlanIsRejected() {
        var plan = draft();
        plan.setStatus("READY");
        plan.setEnvironments("TEST"); // 未选择 PRE
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        assertThatThrownBy(() -> service.startEnvironmentReleaseChecked(plan, 1L, "PRE"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("environment PRE not selected");
    }

    @Test
    void concurrentStartThrowsConflict() {
        var plan = draft();
        plan.setStatus("READY");
        when(taskRepository.findByReleasePlanIdAndEnvironmentCode(1L, "TEST")).thenReturn(Optional.empty());
        doThrow(new ConflictException("operation already running"))
                .when(lockService).lockOrThrow(startsWith("release:start:"), anyString(), any(Duration.class));

        assertThatThrownBy(() -> service.startEnvironmentReleaseChecked(plan, 1L, "TEST"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void prodRequiresReadyStatus() {
        var plan = draft(); // DRAFT
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        assertThatThrownBy(() -> service.startEnvironmentRelease(1L, "PROD"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not READY");
    }
}
