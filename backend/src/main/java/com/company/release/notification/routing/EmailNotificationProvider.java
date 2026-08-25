package com.company.release.notification.routing;

import com.company.release.alert.notification.NotificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 邮件通知 Provider：第一阶段落日志（SMTP 接入在 Phase 21 Hardening 配置化启用）。
 */
public class EmailNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationProvider.class);

    @Override
    public String channel() {
        return "EMAIL";
    }

    @Override
    public void send(String title, String content, String receiver) {
        // Phase 21：JavaMailSender 按渠道 config 注入 SMTP；当前记录日志保证链路可观测
        log.info("[EMAIL->{}] {}", receiver, title);
    }
}
