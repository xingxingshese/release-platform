package com.company.release.iam;

import com.company.release.common.exception.BusinessException;
import com.company.release.iam.repository.PermissionQueryDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 规范 §56：生产发布权限独立控制；无权限 → PERMISSION_DENIED。
 */
class PermissionCheckerTest {

    private PermissionQueryDao dao;
    private PermissionChecker checker;

    @BeforeEach
    void setUp() {
        dao = mock(PermissionQueryDao.class);
        checker = new PermissionChecker(dao);
    }

    @Test
    void hasPermissionTrue() {
        when(dao.findPermissionCodesByUserId(1L))
                .thenReturn(List.of("release:prod:execute", "project:manage"));
        assertThat(checker.hasPermission(1L, "release:prod:execute")).isTrue();
        assertThat(checker.hasPermission(1L, "config:manage")).isFalse();
    }

    @Test
    void missingPermissionThrowsPermissionDenied() {
        when(dao.findPermissionCodesByUserId(2L)).thenReturn(List.of("project:manage"));
        assertThatThrownBy(() -> checker.checkPermission(2L, "release:prod:execute"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("permission denied")
                .extracting(e -> ((BusinessException) e).getErrorCode().name())
                .isEqualTo("PERMISSION_DENIED");
    }

    @Test
    void superAdminHasWildcard() {
        when(dao.findPermissionCodesByUserId(9L)).thenReturn(List.of("*"));
        assertThat(checker.hasPermission(9L, "anything:at:all")).isTrue();
    }
}
