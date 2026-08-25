package com.company.release.project.repository;

import com.company.release.project.domain.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
    List<ServiceEntity> findByProjectId(Long projectId);
}
