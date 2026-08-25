package com.company.release.jenkins;

import com.company.release.common.redis.IdempotencyService;
import com.company.release.jenkins.application.JenkinsWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ADR-010：Jenkins Webhook 幂等——同一 (server, job, buildNumber) 只处理一次。
 */
class JenkinsWebhookServiceTest {

    private IdempotencyService idempotencyService;
    private JenkinsWebhookService webhookService;
    private JenkinsWebhookService.BuildHandler handler;

    @BeforeEach
    void setUp() {
        idempotencyService = mock(IdempotencyService.class);
        handler = mock(JenkinsWebhookService.BuildHandler.class);
        webhookService = new JenkinsWebhookService(idempotencyService, Duration.ofHours(24));
        // 默认：首次调用返回 null（首次），重复返回快照
    }

    private JenkinsWebhookService.WebhookPayload payload(String status) {
        return new JenkinsWebhookService.WebhookPayload(
                1L, "order-service-test", 582L, "SUCCESS", 10086L, null, null, "TEST");
    }

    @Test
    void firstCallbackIsProcessed() {
        when(idempotencyService.putIfAbsent(anyString(), anyString(), any()))
                .thenReturn(null);
        var result = webhookService.onBuildFinished(payload("SUCCESS"), handler);
        assertThat(result.duplicate()).isFalse();
        verify(handler).onBuildFinished(any());
    }

    @Test
    void duplicateCallbackIgnored() {
        when(idempotencyService.putIfAbsent(anyString(), anyString(), any()))
                .thenReturn("{\"processed\":true}");
        var result = webhookService.onBuildFinished(payload("SUCCESS"), handler);
        assertThat(result.duplicate()).isTrue();
        verify(handler, never()).onBuildFinished(any());
    }
}
