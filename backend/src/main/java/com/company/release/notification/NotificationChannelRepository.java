package com.company.release.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationChannelRepository extends JpaRepository<NotificationChannelEntity, Long> {
    Optional<NotificationChannelEntity> findByCodeAndEnabled(String code, boolean enabled);
}
