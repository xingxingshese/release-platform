package com.company.release.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRuleRepository extends JpaRepository<NotificationRuleEntity, Long> {
    List<NotificationRuleEntity> findByEventTypeInAndEnabled(List<String> eventTypes, boolean enabled);
}
