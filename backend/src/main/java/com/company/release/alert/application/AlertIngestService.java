package com.company.release.alert.application;

import com.company.release.alert.domain.AlertEntity;
import com.company.release.alert.domain.AlertFingerprintBuilder;
import com.company.release.alert.domain.AlertNotifyDecider;
import com.company.release.alert.notification.NotificationProvider;
import com.company.release.alert.repository.AlertRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

/**
 * 报警接入（ADR-007）：Webhook → Normalize → Fingerprint → 去重合并 → 通知。
 * 同指纹未恢复报警合并为一条；通知频率由配置策略决定；ACK 停普通通知但升级照常（升级器独立）。
 */
@Service
public class AlertIngestService {

    public record InboundAlert(String projectKey, String title, String content, String level,
                               String environment, String service, String labels) {
    }

    private final AlertRepository alertRepository;
    private final AlertFingerprintBuilder fingerprintBuilder;
    private final List<NotificationProvider> notificationProviders;
    private final Supplier<LocalDateTime> clock;
    private final int repeatIntervalMinutes;

    @org.springframework.beans.factory.annotation.Autowired
    public AlertIngestService(AlertRepository alertRepository,
                              AlertFingerprintBuilder fingerprintBuilder,
                              List<NotificationProvider> notificationProviders,
                              @Value("${alert.repeat-interval-minutes:5}") int repeatIntervalMinutes) {
        this(alertRepository, fingerprintBuilder, notificationProviders,
                LocalDateTime::now, repeatIntervalMinutes);
    }

    /** 测试构造器：注入时钟。 */
    public AlertIngestService(AlertRepository alertRepository,
                              AlertFingerprintBuilder fingerprintBuilder,
                              List<NotificationProvider> notificationProviders,
                              Supplier<LocalDateTime> clock,
                              int repeatIntervalMinutes) {
        this.alertRepository = alertRepository;
        this.fingerprintBuilder = fingerprintBuilder;
        this.notificationProviders = notificationProviders;
        this.clock = clock;
        this.repeatIntervalMinutes = repeatIntervalMinutes;
    }

    private static final List<String> OPEN_STATUSES = List.of("ALERTING", "ACKNOWLEDGED");

    @Transactional
    public AlertEntity ingest(InboundAlert in) {
        var now = clock.get();
        String fp = fingerprintBuilder.build(in.projectKey(), in.service(), in.environment(), "", in.labels());
        var open = alertRepository
                .findFirstByProjectIdAndFingerprintAndStatusInOrderByLastOccurredAtDesc(1L, fp, OPEN_STATUSES);
        if (open.isPresent()) {
            return mergeOccurrence(open.get(), now);
        }
        return createNew(in, fp, now);
    }

    private AlertEntity createNew(InboundAlert in, String fingerprint, LocalDateTime now) {
        var a = new AlertEntity();
        a.setProjectId(1L); // TODO(projectKey→projectId 路由表，Phase 17 配置中心)
        a.setProjectKey(in.projectKey());
        a.setTitle(in.title());
        a.setContent(in.content());
        a.setLevel(normalizeLevel(in.level()));
        a.setEnvironment(in.environment());
        a.setService(in.service());
        a.setLabels(in.labels());
        a.setFingerprint(fingerprint);
        a.setFirstOccurredAt(now);
        a.setLastOccurredAt(now);
        a.setStatus("ALERTING");
        var saved = alertRepository.save(a);
        notify(saved); // 首次立即通知
        return saved;
    }

    private AlertEntity mergeOccurrence(AlertEntity existing, LocalDateTime now) {
        long minutesSinceLastNotify = Duration.between(existing.getLastOccurredAt(), now).toMinutes();
        boolean acknowledged = existing.getAcknowledgedBy() != null;
        // 已通知次数含创建时的首次通知：count 存 0 表示已发过 1 条
        boolean shouldNotify = new AlertNotifyDecider(new com.company.release.alert.domain.AlertNotificationPolicy(
                repeatIntervalMinutes, 3))
                .shouldSendRepeatNotification(existing.getNotifiedRepeatCount() + 1, minutesSinceLastNotify, acknowledged);
        existing.setLastOccurredAt(now);
        if (shouldNotify) {
            existing.setNotifiedRepeatCount(existing.getNotifiedRepeatCount() + 1);
            notify(existing);
        }
        return alertRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public java.util.List<AlertEntity> listByProject(Long projectId) {
        return alertRepository.findByProjectId(projectId);
    }

    /** ACK：停止普通重复通知，但升级不受影响（EscalationDecider 独立判断）。 */
    @Transactional
    public void acknowledge(Long operatorId, Long alertId) {
        var alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new com.company.release.common.exception.BusinessException(
                        com.company.release.common.exception.ErrorCode.NOT_FOUND, "alert not found: " + alertId));
        if (!"RESOLVED".equals(alert.getStatus())) {
            alert.setStatus("ACKNOWLEDGED");
            alert.setAcknowledgedBy(operatorId);
            alert.setAcknowledgedAt(clock.get());
            alertRepository.save(alert);
        }
    }

    /** 恢复：外部系统恢复事件。 */
    @Transactional
    public void resolve(Long operatorId, Long alertId) {
        var alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new com.company.release.common.exception.BusinessException(
                        com.company.release.common.exception.ErrorCode.NOT_FOUND, "alert not found: " + alertId));
        if (!"RESOLVED".equals(alert.getStatus())) {
            alert.setStatus("RESOLVED");
            alert.setResolvedAt(clock.get());
            alertRepository.save(alert);
            notifyResolved(alert);
        }
    }

    private void notify(AlertEntity alert) {
        for (var provider : notificationProviders) {
            try {
                provider.send("[报警] " + alert.getTitle(),
                        "%s/%s level=%s env=%s".formatted(
                                alert.getProjectKey(), alert.getService(),
                                alert.getLevel(), alert.getEnvironment()),
                        "ALERT_OWNER"); // 接收人路由由 AlertRoute 配置驱动，Phase 17 接入
            } catch (Exception e) {
                // 通知失败不影响主流程（ADR-009），由重试机制补偿
            }
        }
    }

    private void notifyResolved(AlertEntity alert) {
        for (var provider : notificationProviders) {
            try {
                provider.send("[恢复] " + alert.getTitle(),
                        "resolved at " + alert.getResolvedAt(), "ALERT_OWNER");
            } catch (Exception ignored) {
            }
        }
    }

    private static String normalizeLevel(String level) {
        return switch (level == null ? "" : level.toUpperCase()) {
            case "INFO", "WARNING", "ERROR", "CRITICAL" -> level.toUpperCase();
            default -> "WARNING";
        };
    }
}
