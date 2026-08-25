package com.company.release.alert.repository;

import com.company.release.alert.domain.AlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<AlertEntity, Long> {

    /** 未恢复（ALERTING/ACKNOWLEDGED）的同指纹报警 → 去重合并目标。 */
    Optional<AlertEntity> findFirstByProjectIdAndFingerprintAndStatusInOrderByLastOccurredAtDesc(
            Long projectId, String fingerprint, List<String> statuses);

    List<AlertEntity> findByProjectId(Long projectId);

    List<AlertEntity> findByStatusIn(List<String> statuses);
}
