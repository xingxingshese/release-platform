package com.company.release.release.application;

import com.company.release.common.exception.BusinessException;
import com.company.release.iam.PermissionChecker;
import com.company.release.iam.repository.PermissionQueryDao;
import com.company.release.release.domain.model.PlanServiceEntity;
import com.company.release.release.domain.model.ReleasePlanEntity;
import com.company.release.release.repository.PlanServiceRepository;
import com.company.release.release.repository.ReleasePlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Phase 9/10：验收守卫 + Release Branch 规则（规范 §16/§17）。
 */
class AcceptanceAndBranchTest {

    private ReleasePlanRepository planRepository;
    private PlanServiceRepository planServiceRepository;
    private PermissionQueryDao permissionDao;
    private AcceptanceService acceptanceService;
    private ReleaseBranchService branchService;

    @BeforeEach
    void setUp() {
        planRepository = mock(ReleasePlanRepository.class);
        planServiceRepository = mock(PlanServiceRepository.class);
        permissionDao = mock(PermissionQueryDao.class);
        var checker = new PermissionChecker(permissionDao);
        acceptanceService = new AcceptanceService(planRepository, checker);
        branchService = new ReleaseBranchService(planRepository, planServiceRepository, checker,
                "release_{yyyyMMdd}_{releasePlanId}");
        when(permissionDao.findPermissionCodesByUserId(anyLong())).thenReturn(List.of("*"));
    }

    private ReleasePlanEntity plan(String status) {
        var p = new ReleasePlanEntity();
        p.setId(1L);
        p.setStatus(status);
        return p;
    }

    private PlanServiceEntity branch(String existingReleaseBranch) {
        var b = new PlanServiceEntity();
        b.setId(9L);
        b.setReleasePlanId(1L);
        b.setSourceBranch("feature/order-123");
        b.setTargetTestBranch("release_test");
        b.setReleaseBranch(existingReleaseBranch);
        return b;
    }

    // ---- Phase 9: 验收 ----

    @Test
    void acceptOnlyAllowedInWaitTestAccept() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan("TEST_DEPLOY_SUCCESS")));
        assertThatThrownBy(() -> acceptanceService.accept(7L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("WAIT_TEST_ACCEPT");
    }

    @Test
    void acceptTransitsToAccepted() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan("WAIT_TEST_ACCEPT")));
        acceptanceService.accept(7L, 1L);
        assertThat(planRepository.findById(1L).orElseThrow().getStatus()).isEqualTo("TEST_ACCEPTED");
    }

    @Test
    void rejectTransitsToRejectedAndCanReturnToReady() {
        var p = plan("WAIT_TEST_ACCEPT");
        when(planRepository.findById(1L)).thenReturn(Optional.of(p));
        acceptanceService.reject(7L, 1L);
        assertThat(p.getStatus()).isEqualTo("TEST_REJECTED");
    }

    @Test
    void repeatedAcceptIsIdempotent() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan("TEST_ACCEPTED")));
        acceptanceService.accept(7L, 1L); // 不抛异常、不重复保存
        verify(planRepository, never()).save(any());
    }

    // ---- Phase 10: Release Branch ----

    @Test
    void branchRequiresAcceptedPlan() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan("WAIT_TEST_ACCEPT")));
        assertThatThrownBy(() -> branchService.create(7L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TEST_ACCEPTED");
    }

    @Test
    void branchNameFollowsConfiguredTemplate() {
        String name = branchService.branchName(plan("TEST_ACCEPTED"));
        String expectedPrefix = "release_"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "_";
        assertThat(name).startsWith(expectedPrefix).endsWith("_1");
    }

    @Test
    void createWritesBranchToAllPlanServices() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan("TEST_ACCEPTED")));
        var b1 = branch(null);
        var b2 = branch(null);
        when(planServiceRepository.findByReleasePlanId(1L)).thenReturn(List.of(b1, b2));

        branchService.create(7L, 1L);

        assertThat(b1.getReleaseBranch()).isEqualTo(b2.getReleaseBranch());
        assertThat(b1.getReleaseBranch()).startsWith("release_");
        verify(planServiceRepository).saveAll(List.of(b1, b2));
    }

    @Test
    void createIsIdempotentWhenBranchAlreadyExists() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan("TEST_ACCEPTED")));
        when(planServiceRepository.findByReleasePlanId(1L))
                .thenReturn(List.of(branch("release_20260824_10086")));

        branchService.create(7L, 1L); // 二次调用：不抛异常不改写
        verify(planServiceRepository, never()).saveAll(any());
    }
}
