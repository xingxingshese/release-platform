package com.company.release.jenkins.application;

import com.company.release.common.redis.IdempotencyService;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Jenkins Webhook 处理（ADR-010）：唯一键 jenkins:{serverId}:{job}:{buildNumber}。
 */
@Service
public class JenkinsWebhookService {

    public record WebhookPayload(Long serverId, String jobName, Long buildNumber, String status,
                                 Long releasePlanId, Long releaseTaskId, Long serviceId, String environment) {
    }

    public record ProcessResult(boolean duplicate, String firstResult) {
    }

    /** 编排层注入：首次回调时更新 ReleaseTask 状态。 */
    public interface BuildHandler {
        void onBuildFinished(WebhookPayload payload);
    }

    private final IdempotencyService idempotencyService;
    private final Duration ttl;

    public JenkinsWebhookService(IdempotencyService idempotencyService,
                                 @org.springframework.beans.factory.annotation.Value("${jenkins.webhook.idempotency-ttl:PT24H}")
                                 Duration ttl) {
        this.idempotencyService = idempotencyService;
        this.ttl = ttl;
    }

    /**
     * @return duplicate=true 表示重复回调（已处理过），直接忽略。
     */
    public ProcessResult onBuildFinished(WebhookPayload payload, BuildHandler handler) {
        String key = "jenkins:webhook:%d:%s:%d"
                .formatted(payload.serverId(), payload.jobName(), payload.buildNumber());
        String snapshot = "{\"processed\":true,\"status\":\"%s\"}".formatted(payload.status());
        String existing = idempotencyService.putIfAbsent(key, snapshot, ttl);
        if (existing != null) {
            return new ProcessResult(true, existing);
        }
        try {
            handler.onBuildFinished(payload);
            return new ProcessResult(false, null);
        } catch (RuntimeException e) {
            // 处理失败则释放幂等键，允许重试投递
            idempotencyService.storeResult(key, "{\"failed\":true}", Duration.ZERO);
            throw e;
        }
    }
}
