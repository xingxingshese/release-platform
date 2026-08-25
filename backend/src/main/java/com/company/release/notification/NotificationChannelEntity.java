package com.company.release.notification;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 通知渠道配置（表 V9）。config 内 webhook/secret 加密存储。 */
@Entity
@Table(name = "notification_channel")
public class NotificationChannelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    /** WECOM / FEISHU / EMAIL / INTERNAL */
    @Column(nullable = false)
    private String type;

    @Column(columnDefinition = "json", nullable = false)
    private String config;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String v) { this.code = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public String getConfig() { return config; }
    public void setConfig(String v) { this.config = v; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }
}
