package com.company.release.alert.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 站内/日志通知（第一期兜底渠道）：记录到日志与 notification_record。
 * 企业微信/飞书 Provider 在后续 Phase 接入真实 API。
 */
@Component
public class LogNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationProvider.class);

    @Override
    public String channel() {
        return "INTERNAL";
    }

    @Override
    public void send(String title, String content, String receiver) {
        log.info("[NOTIFY][INTERNAL] to={} title={} content={}", receiver, title,
                content.length() > 200 ? content.substring(0, 200) + "..." : content);
    }
}
