package com.company.release.release.repository;

import com.company.release.release.domain.model.ReleasePlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleasePlanRepository extends JpaRepository<ReleasePlanEntity, Long> {
}
