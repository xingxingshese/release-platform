package com.company.release.notification;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 通知路由规则（表 V9）：事件×项目×环境×级别 → 渠道+接收人，全部配置化。 */
@Entity
@Table(name = "notification_rule")
public class NotificationRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** RELEASE_SUCCESS / RELEASE_FAILED / ALERT / ALERT_ESCALATED / ALERT_RESOLVED / * */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    /** NULL = 全部项目 */
    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "environment_code")
    private String environmentCode;

    @Column(name = "min_level", nullable = false)
    private String minLevel = "INFO";

    @Column(name = "channel_code", nullable = false)
    private String channelCode;

    @Column(name = "receiver_template", nullable = false)
    private String receiverTemplate;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getEventType() { return eventType; }
    public void setEventType(String v) { this.eventType = v; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long v) { this.projectId = v; }
    public String getEnvironmentCode() { return environmentCode; }
    public void setEnvironmentCode(String v) { this.environmentCode = v; }
    public String getMinLevel() { return minLevel; }
    public void setMinLevel(String v) { this.minLevel = v; }
    public String getChannelCode() { return channelCode; }
    public void setChannelCode(String v) { this.channelCode = v; }
    public String getReceiverTemplate() { return receiverTemplate; }
    public void setReceiverTemplate(String v) { this.receiverTemplate = v; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }
}
