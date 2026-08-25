package com.company.release.requirement.repository;

import com.company.release.requirement.domain.RequirementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RequirementRepository extends JpaRepository<RequirementEntity, Long> {
    Optional<RequirementEntity> findByProjectIdAndSourceTypeAndExternalId(
            Long projectId, String sourceType, String externalId);

    List<RequirementEntity> findByProjectId(Long projectId);
}
