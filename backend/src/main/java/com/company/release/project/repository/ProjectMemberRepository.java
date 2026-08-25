package com.company.release.project.repository;

import com.company.release.project.domain.ProjectMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMemberEntity, Long> {
    List<ProjectMemberEntity> findByProjectId(Long projectId);
    boolean existsByProjectIdAndUserIdAndRole(Long projectId, Long userId, String role);
}
