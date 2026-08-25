package com.company.release.release.repository;

import com.company.release.release.domain.model.ReleaseTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReleaseTaskRepository extends JpaRepository<ReleaseTaskEntity, Long> {
    Optional<ReleaseTaskEntity> findByReleasePlanIdAndEnvironmentCode(Long releasePlanId, String environmentCode);

    Optional<ReleaseTaskEntity> findByJenkinsServerIdAndJenkinsJobNameAndJenkinsBuildNumber(
            Long serverId, String jobName, Long buildNumber);
}
