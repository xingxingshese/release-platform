package com.company.release.notification.routing;

import com.company.release.alert.notification.NotificationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 渠道 Provider 注册（规范 §八：企业微信/飞书第一阶段即支持；EMAIL 内建）。
 * INTERNAL 渠道由 alert 模块的 LogNotificationProvider 提供，避免重复注册。
 */
@Configuration
public class NotificationProvidersConfig {

    @Bean
    public NotificationProvider wecomNotificationProvider() {
        return new WebhookNotificationProvider("WECOM");
    }

    @Bean
    public NotificationProvider feishuNotificationProvider() {
        return new WebhookNotificationProvider("FEISHU");
    }

    @Bean
    public NotificationProvider emailNotificationProvider() {
        return new EmailNotificationProvider();
    }
}
