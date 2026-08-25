package com.company.release.notification.routing;

import com.company.release.alert.notification.NotificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 企业微信/飞书 Webhook 通知 Provider（规范 §八：企业微信/飞书）。
 * webhook 地址经 receiver 传入（来自渠道配置）；发送失败抛出由 Dispatcher 捕获记录，不阻断主流程。
 */
public class WebhookNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(WebhookNotificationProvider.class);

    private final String channel;
    private final RestClient restClient;

    public WebhookNotificationProvider(String channel) {
        this(channel, RestClient.create());
    }

    public WebhookNotificationProvider(String channel, RestClient restClient) {
        this.channel = channel;
        this.restClient = restClient;
    }

    @Override
    public String channel() {
        return channel;
    }

    @Override
    public void send(String title, String content, String receiver) {
        try {
            // 企业微信与飞书机器人均为 {"msgtype":"text","text":{"content":"..."}} 兼容体
            Map<String, Object> payload = Map.of(
                    "msgtype", "text",
                    "text", Map.of("content", (title == null || title.isBlank() ? "" : title + "\n") + content));
            String resp = restClient.post()
                    .uri(receiver)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            log.info("notification sent via {} resp={}", channel, resp);
        } catch (Exception e) {
            throw new IllegalStateException(channel + " notify failed: " + e.getMessage(), e);
        }
    }
}
