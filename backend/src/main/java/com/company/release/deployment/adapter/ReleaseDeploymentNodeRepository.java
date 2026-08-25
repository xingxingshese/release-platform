package com.company.release.deployment.adapter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReleaseDeploymentNodeRepository extends JpaRepository<ReleaseDeploymentNodeEntity, Long> {

    List<ReleaseDeploymentNodeEntity> findByReleaseTaskId(Long releaseTaskId);
}
