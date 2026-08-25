package com.company.release.project.repository;

import com.company.release.project.domain.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {
    boolean existsByCodeIgnoreCase(String code);
}
