package com.company.release.notification.routing;

import com.company.release.alert.notification.NotificationProvider;
import com.company.release.common.observability.ReleaseMetrics;
import com.company.release.notification.NotificationRecordEntity;
import com.company.release.notification.NotificationRecordRepository;
import com.company.release.notification.NotificationRuleEntity;
import com.company.release.notification.NotificationRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;

/**
 * 通知分发器（spec 013 / ADR-009）：
 * 路由（事件×项目×环境×级别）→ Provider 发送 → 落 notification_record → 指标。
 * 异步执行；任何渠道失败仅记录，绝不影响发布/报警主流程。
 */
@Service
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationRuleRepository ruleRepository;
    private final NotificationRecordRepository recordRepository;
    private final ReleaseMetrics metrics;
    /** channel code → provider（含测试注入的 Fake）。 */
    private final Map<String, NotificationProvider> providers;

    public NotificationDispatcher(NotificationRuleRepository ruleRepository,
                                  NotificationRecordRepository recordRepository,
                                  ReleaseMetrics metrics,
                                  List<NotificationProvider> providers) {
        this.ruleRepository = ruleRepository;
        this.recordRepository = recordRepository;
        this.metrics = metrics;
        this.providers = providers.stream()
                .collect(java.util.stream.Collectors.toMap(NotificationProvider::channel, p -> p, (a, b) -> a));
    }

    /** 匹配规则：event_type 精确或 *；project/environment NULL=全部。静态便于单测。 */
    static boolean matches(NotificationRuleEntity rule, String eventType, Long projectId, String envCode, String level) {
        if (!rule.isEnabled()) {
            return false;
        }
        if (!"*".equals(rule.getEventType()) && !rule.getEventType().equals(eventType)) {
            return false;
        }
        if (rule.getProjectId() != null && projectId != null && !rule.getProjectId().equals(projectId)) {
            return false;
        }
        if (rule.getEnvironmentCode() != null && envCode != null && !rule.getEnvironmentCode().equals(envCode)) {
            return false;
        }
        return levelRank(level) >= levelRank(rule.getMinLevel());
    }

    private static int levelRank(String level) {
        return switch (level == null ? "INFO" : level) {
            case "CRITICAL" -> 2;
            case "WARN" -> 1;
            default -> 0;
        };
    }

    /**
     * 异步发送入口：主流程调用后立即返回。
     */
    @Async
    public void dispatch(String eventType, Long projectId, String envCode, String level,
                         String title, String content, String relatedType, String relatedId) {
        var matched = new ArrayList<NotificationRuleEntity>();
        for (var rule : ruleRepository.findByEventTypeInAndEnabled(List.of(eventType, "*"), true)) {
            if (matches(rule, eventType, projectId, envCode, level)) {
                matched.add(rule);
            }
        }
        if (matched.isEmpty()) {
            log.debug("no notification rule matched event={}", eventType);
            return;
        }
        for (var rule : matched) {
            sendVia(rule.getChannelCode(), rule.getReceiverTemplate(),
                    title, content, relatedType, relatedId);
        }
    }

    public void sendVia(String channelCode, String receiver, String title, String content,
                 String relatedType, String relatedId) {
        var provider = providers.get(channelCode);
        var record = new NotificationRecordEntity();
        record.setChannel(channelCode);
        record.setReceiver(receiver);
        record.setTitle(title);
        record.setContent(content);
        record.setRelatedType(relatedType);
        record.setRelatedId(relatedId);
        try {
            if (provider == null) {
                throw new IllegalStateException("no provider for channel: " + channelCode);
            }
            provider.send(title, content, receiver);
            record.setSuccess(true);
            metrics.notificationSent(true);
        } catch (Exception e) {
            log.warn("notification failed channel={} err={}", channelCode, e.getMessage());
            record.setSuccess(false);
            record.setErrorMessage(e.getMessage());
            metrics.notificationSent(false);
        }
        recordRepository.save(record);
    }

    /** 供测试断言已注册渠道。 */
    boolean hasProvider(String channel) {
        return providers.containsKey(channel);
    }
}
