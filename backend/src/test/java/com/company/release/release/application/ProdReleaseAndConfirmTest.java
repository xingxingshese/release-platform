package com.company.release.release.application;

import com.company.release.common.exception.BusinessException;
import com.company.release.iam.PermissionChecker;
import com.company.release.iam.repository.PermissionQueryDao;
import com.company.release.release.domain.model.ReleasePlanEntity;
import com.company.release.release.repository.ReleasePlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Phase 11/12：生产发布守卫与确认（规范 §18/§39、agent.md 红线）。
 * 生产发布权限独立控制；PRE→PROD 依赖可配置；确认幂等。
 */
class ProdReleaseAndConfirmTest {

    private ReleasePlanRepository planRepository;
    private PermissionQueryDao permissionDao;
    private ProdConfirmService service;

    @BeforeEach
    void setUp() {
        planRepository = mock(ReleasePlanRepository.class);
        permissionDao = mock(PermissionQueryDao.class);
        var checker = new PermissionChecker(permissionDao);
        service = new ProdConfirmService(planRepository, checker, false);
    }

    private ReleasePlanEntity plan(String status) {
        var p = new ReleasePlanEntity();
        p.setId(1L);
        p.setStatus(status);
        return p;
    }

    @Test
    void prodExecuteRequiresDedicatedPermission() {
        // 用户只有 release:edit，无 release:prod:execute → PERMISSION_DENIED
        when(permissionDao.findPermissionCodesByUserId(anyLong())).thenReturn(List.of("release:edit"));
        assertThatThrownBy(() -> ProdConfirmService.checkProdPermission(7L, new PermissionChecker(permissionDao)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("release:prod:execute");
    }

    @Test
    void prodStartRequiresPreSuccessWhenParallelDisabled() {
        assertThat(ProdConfirmService.prodStartAllowed(
                ProdConfirmService.planState("READY"), false))
                .isFalse(); // 并行关闭：必须 PRE_DEPLOY_SUCCESS
        assertThat(ProdConfirmService.prodStartAllowed(
                ProdConfirmService.planState("PRE_DEPLOY_SUCCESS"), false))
                .isTrue();
    }

    @Test
    void prodStartAllowedFromReadyWhenParallelEnabled() {
        assertThat(ProdConfirmService.prodStartAllowed(
                ProdConfirmService.planState("READY"), true))
                .isTrue();
    }

    @Test
    void confirmOnlyInWaitProdConfirm() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan("PROD_DEPLOY_SUCCESS")));
        when(permissionDao.findPermissionCodesByUserId(anyLong())).thenReturn(List.of("*"));
        assertThatThrownBy(() -> service.confirm(7L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("WAIT_PROD_CONFIRM");
    }

    @Test
    void confirmTransitsToCompleted() {
        var p = plan("WAIT_PROD_CONFIRM");
        when(planRepository.findById(1L)).thenReturn(Optional.of(p));
        when(permissionDao.findPermissionCodesByUserId(anyLong())).thenReturn(List.of("*"));
        service.confirm(7L, 1L);
        assertThat(p.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void repeatedConfirmIsIdempotent() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan("COMPLETED")));
        when(permissionDao.findPermissionCodesByUserId(anyLong())).thenReturn(List.of("*"));
        service.confirm(7L, 1L); // 不抛异常不保存
        verify(planRepository, never()).save(any());
    }
}
