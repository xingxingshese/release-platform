package com.company.release.project.application;

import com.company.release.common.exception.BusinessException;
import com.company.release.common.exception.ErrorCode;
import com.company.release.iam.PermissionChecker;
import com.company.release.project.domain.ProjectEntity;
import com.company.release.project.domain.ProjectMemberEntity;
import com.company.release.project.domain.ServiceEntity;
import com.company.release.project.repository.ProjectMemberRepository;
import com.company.release.project.repository.ProjectRepository;
import com.company.release.project.repository.ServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 项目管理（规范 §4/§5）。变更操作要求 project:manage 权限。
 */
@Service
public class ProjectService {

    public static final Set<String> PROJECT_TYPES = Set.of("BACKEND", "FRONTEND", "FULLSTACK", "MIXED");
    public static final Set<String> SERVICE_TYPES = Set.of("BACKEND", "FRONTEND", "OTHER");

    private final ProjectRepository projectRepository;
    private final ServiceRepository serviceRepository;
    private final ProjectMemberRepository memberRepository;
    private final PermissionChecker permissionChecker;

    public ProjectService(ProjectRepository projectRepository,
                          ServiceRepository serviceRepository,
                          ProjectMemberRepository memberRepository,
                          PermissionChecker permissionChecker) {
        this.projectRepository = projectRepository;
        this.serviceRepository = serviceRepository;
        this.memberRepository = memberRepository;
        this.permissionChecker = permissionChecker;
    }

    public record CreateProjectCmd(String code, String name, String description, String projectType) {
    }

    public record AddServiceCmd(String code, String name, String type) {
    }

    @Transactional
    public ProjectEntity create(Long operatorId, CreateProjectCmd cmd) {
        permissionChecker.checkPermission(operatorId, "project:manage");
        if (cmd.code() == null || cmd.code().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "project code is required");
        }
        if (!PROJECT_TYPES.contains(cmd.projectType())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "invalid project type: " + cmd.projectType() + ", allowed: " + PROJECT_TYPES);
        }
        if (projectRepository.existsByCodeIgnoreCase(cmd.code())) {
            throw new BusinessException(ErrorCode.CONFLICT, "project code already exists: " + cmd.code());
        }
        ProjectEntity p = new ProjectEntity();
        p.setCode(cmd.code());
        p.setName(cmd.name());
        p.setDescription(cmd.description());
        p.setProjectType(cmd.projectType());
        p.setOwnerId(operatorId);
        return projectRepository.save(p);
    }

    @Transactional(readOnly = true)
    public ProjectEntity getById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "project not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ProjectEntity> list() {
        return projectRepository.findAll();
    }

    @Transactional
    public void addService(Long projectId, Long operatorId, AddServiceCmd cmd) {
        permissionChecker.checkPermission(operatorId, "project:manage");
        if (!SERVICE_TYPES.contains(cmd.type())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "invalid service type: " + cmd.type() + ", allowed: " + SERVICE_TYPES);
        }
        getById(projectId);
        ServiceEntity s = new ServiceEntity();
        s.setProjectId(projectId);
        s.setCode(cmd.code());
        s.setName(cmd.name());
        s.setType(cmd.type());
        serviceRepository.save(s);
    }

    @Transactional(readOnly = true)
    public List<ServiceEntity> services(Long projectId) {
        return serviceRepository.findByProjectId(projectId);
    }

    /** 同一 (project,user,role) 重复添加幂等（ADR-010 业务唯一键）。 */
    @Transactional
    public void addMember(Long projectId, Long userId, String role) {
        validateRole(role);
        if (memberRepository.existsByProjectIdAndUserIdAndRole(projectId, userId, role)) {
            return; // 幂等：重复添加直接成功返回
        }
        var m = new ProjectMemberEntity();
        m.setProjectId(projectId);
        m.setUserId(userId);
        m.setRole(role);
        memberRepository.save(m);
    }

    private void validateRole(String role) {
        if (!Set.of(ProjectMemberEntity.PROJECT_OWNER, ProjectMemberEntity.DEVELOPER,
                ProjectMemberEntity.TESTER, ProjectMemberEntity.PRODUCT,
                ProjectMemberEntity.RELEASE_OWNER, ProjectMemberEntity.ALERT_OWNER).contains(role)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "invalid role: " + role);
        }
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberEntity> members(Long projectId) {
        return memberRepository.findByProjectId(projectId);
    }
}
