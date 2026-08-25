package com.company.release.alert.notification;

/**
 * 通知渠道 Provider 抽象（规范 §37）：企业微信/飞书/钉钉/邮件/站内。
 * 通知失败不得影响发布/报警主流程（ADR-009），由调用方异步+重试。
 */
public interface NotificationProvider {

    /** WECOM / FEISHU / DINGTALK / EMAIL / INTERNAL */
    String channel();

    void send(String title, String content, String receiver);
}
