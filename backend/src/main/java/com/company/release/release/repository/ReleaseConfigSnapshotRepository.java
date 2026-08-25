package com.company.release.release.repository;

import com.company.release.release.domain.model.ReleaseConfigSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseConfigSnapshotRepository extends JpaRepository<ReleaseConfigSnapshotEntity, Long> {
}
