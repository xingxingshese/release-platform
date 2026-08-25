package com.company.release.alert.escalation;

import com.company.release.alert.domain.AlertEntity;
import com.company.release.alert.domain.EscalationDecider;
import com.company.release.alert.domain.EscalationLevel;
import com.company.release.alert.notification.NotificationProvider;
import com.company.release.alert.repository.AlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

/**
 * 报警升级调度（规范 §50 / ADR-007）：
 * 周期扫描未恢复报警，按 elapsed >= level.delay 逐级升级；ACK 不阻止升级；RESOLVED 终止。
 */
@Service
public class AlertEscalationJob {

    private static final Logger log = LoggerFactory.getLogger(AlertEscalationJob.class);

    private final AlertRepository alertRepository;
    private final List<NotificationProvider> notificationProviders;
    private Supplier<LocalDateTime> clock = LocalDateTime::now;
    private final List<EscalationLevel> levels;

    public AlertEscalationJob(AlertRepository alertRepository,
                              List<NotificationProvider> notificationProviders,
                              @Value("${alert.escalation.levels:1:0:ALERT_OWNER,2:10:TECH_LEAD,3:20:PROJECT_OWNER}")
                              String levelsCsv) {
        this.alertRepository = alertRepository;
        this.notificationProviders = notificationProviders;
        this.levels = parseLevels(levelsCsv);
    }

    /** 测试用：注入时钟。 */
    void setClock(Supplier<LocalDateTime> clock) {
        this.clock = clock;
    }

    /** levelsCsv 形如 "level:delayMinutes:receiverRole,..."（配置驱动，禁止硬编码负责人）。 */
    static List<EscalationLevel> parseLevels(String csv) {
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    var p = s.split(":");
                    return new EscalationLevel(Integer.parseInt(p[0]), Integer.parseInt(p[1]), p[2]);
                })
                .sorted(java.util.Comparator.comparingInt(EscalationLevel::delayMinutes))
                .toList();
    }

    @Scheduled(fixedDelayString = "${alert.escalation.scan-seconds:60}000")
    public void scan() {
        processEscalations();
    }

    public void processEscalations() {
        var now = clock.get();
        var decider = new EscalationDecider(levels);
        for (var alert : alertRepository.findByStatusIn(List.of("ALERTING", "ACKNOWLEDGED"))) {
            long elapsed = Duration.between(alert.getFirstOccurredAt(), now).toMinutes();
            decider.nextLevel(elapsed, alert.getEscalatedToLevel(),
                    alert.getAcknowledgedBy() != null, false)
                    .ifPresent(level -> escalate(alert, level));
        }
    }

    void escalate(AlertEntity alert, int level) {
        var target = levels.stream().filter(l -> l.level() == level).findFirst().orElseThrow();
        alert.setEscalatedToLevel(level);
        alertRepository.save(alert);
        for (var provider : notificationProviders) {
            try {
                provider.send("[报警升级 L%d] %s".formatted(level, alert.getTitle()),
                        "已持续未恢复，升级至 %s".formatted(target.receiverRole()), target.receiverRole());
            } catch (Exception e) {
                log.warn("escalation notify failed via {}: {}", provider.channel(), e.getMessage());
            }
        }
    }
}
