package com.company.release.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConfigVersionRepository extends JpaRepository<ConfigVersionEntity, Long> {

    Optional<ConfigVersionEntity> findFirstByConfigTypeAndConfigKeyOrderByVersionDesc(
            String configType, String configKey);

    List<ConfigVersionEntity> findByConfigTypeAndConfigKeyOrderByVersionDesc(String configType, String configKey);
}
