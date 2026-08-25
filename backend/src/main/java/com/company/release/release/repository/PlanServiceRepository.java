package com.company.release.release.repository;

import com.company.release.release.domain.model.PlanServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanServiceRepository extends JpaRepository<PlanServiceEntity, Long> {
    List<PlanServiceEntity> findByReleasePlanId(Long releasePlanId);
}
