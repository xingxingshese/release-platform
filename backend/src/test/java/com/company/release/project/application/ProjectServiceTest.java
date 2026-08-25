package com.company.release.project.application;

import com.company.release.common.exception.BusinessException;
import com.company.release.iam.PermissionChecker;
import com.company.release.project.domain.ProjectEntity;
import com.company.release.project.domain.ProjectMemberEntity;
import com.company.release.project.domain.ServiceEntity;
import com.company.release.project.repository.ProjectMemberRepository;
import com.company.release.project.repository.ProjectRepository;
import com.company.release.project.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectServiceTest {

    private ProjectRepository projectRepository;
    private ServiceRepository serviceRepository;
    private ProjectMemberRepository memberRepository;
    private PermissionChecker permissionChecker;
    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        serviceRepository = mock(ServiceRepository.class);
        memberRepository = mock(ProjectMemberRepository.class);
        permissionChecker = mock(PermissionChecker.class);
        projectService = new ProjectService(projectRepository, serviceRepository, memberRepository, permissionChecker);
        doNothing().when(permissionChecker).checkPermission(any(), any());
    }

    @Test
    void createProjectRejectsDuplicateCodeCaseInsensitive() {
        when(projectRepository.existsByCodeIgnoreCase("order")).thenReturn(true);
        assertThatThrownBy(() -> projectService.create(1L, cmd("order")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createProjectRejectsInvalidType() {
        var cmd = new ProjectService.CreateProjectCmd("order", "订单", null, "INVALID_TYPE");
        assertThatThrownBy(() -> projectService.create(1L, cmd))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("project type");
    }

    @Test
    void createProjectSucceedsAndSetsDefaults() {
        when(projectRepository.existsByCodeIgnoreCase("order")).thenReturn(false);
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var saved = projectService.create(1L, cmd("order"));
        assertThat(saved.getCode()).isEqualTo("order");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getOwnerId()).isEqualTo(1L);
        verify(permissionChecker).checkPermission(1L, "project:manage");
    }

    @Test
    void getUnknownProjectThrowsNotFound() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> projectService.getById(99L))
                .hasMessageContaining("not found");
    }

    @Test
    void addMemberValidatesRole() {
        when(memberRepository.existsByProjectIdAndUserIdAndRole(1L, 2L, "BOSS")).thenReturn(false);
        assertThatThrownBy(() -> projectService.addMember(1L, 2L, "BOSS"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("invalid role");
    }

    @Test
    void addMemberIsIdempotentForSameTriple() {
        when(memberRepository.existsByProjectIdAndUserIdAndRole(1L, 2L, ProjectMemberEntity.DEVELOPER)).thenReturn(true);
        projectService.addMember(1L, 2L, ProjectMemberEntity.DEVELOPER);
        verify(memberRepository, never()).save(any());
    }

    @Test
    void addMemberAcceptsAllDefinedRoles() {
        when(memberRepository.existsByProjectIdAndUserIdAndRole(any(), any(), any())).thenReturn(false);
        for (String role : Set.of(ProjectMemberEntity.PROJECT_OWNER, ProjectMemberEntity.DEVELOPER,
                ProjectMemberEntity.TESTER, ProjectMemberEntity.PRODUCT,
                ProjectMemberEntity.RELEASE_OWNER, ProjectMemberEntity.ALERT_OWNER)) {
            projectService.addMember(1L, 2L, role);
        }
        verify(memberRepository, times(6)).save(any(ProjectMemberEntity.class));
    }

    @Test
    void addServiceValidatesType() {
        assertThatThrownBy(() -> projectService.addService(1L, 1L, new ProjectService.AddServiceCmd(
                "order-web", "订单前端", "MICROSERVICE")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("service type");
    }

    private ProjectService.CreateProjectCmd cmd(String code) {
        return new ProjectService.CreateProjectCmd(code, "订单项目", "desc", "BACKEND");
    }
}
