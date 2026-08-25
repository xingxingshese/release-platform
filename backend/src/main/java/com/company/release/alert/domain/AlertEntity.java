package com.company.release.alert.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 统一报警模型（规范 §45）。 */
@Entity
@Table(name = "alert")
public class AlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "project_key", length = 64)
    private String projectKey;

    @Column(nullable = false, length = 32)
    private String source = "CUSTOM_WEBHOOK";

    @Column(name = "external_alert_id", length = 128)
    private String externalAlertId;

    @Column(nullable = false)
    private String title;

    @Column(length = 2048)
    private String content;

    /** INFO/WARNING/ERROR/CRITICAL */
    @Column(nullable = false, length = 16)
    private String level;

    /** ALERTING/ACKNOWLEDGED/RESOLVED */
    @Column(nullable = false, length = 24)
    private String status = "ALERTING";

    @Column(length = 64)
    private String environment;

    @Column(length = 128)
    private String service;

    @Column(columnDefinition = "JSON")
    private String labels;

    @Column(nullable = false, columnDefinition = "char(64)")
    private String fingerprint;

    @Column(name = "notified_repeat_count", nullable = false)
    private int notifiedRepeatCount = 0;

    @Column(name = "escalated_to_level", nullable = false)
    private int escalatedToLevel = 0;

    @Column(name = "first_occurred_at", nullable = false)
    private LocalDateTime firstOccurredAt;

    @Column(name = "last_occurred_at", nullable = false)
    private LocalDateTime lastOccurredAt;

    @Column(name = "acknowledged_by")
    private Long acknowledgedBy;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectKey() { return projectKey; }
    public void setProjectKey(String projectKey) { this.projectKey = projectKey; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getExternalAlertId() { return externalAlertId; }
    public void setExternalAlertId(String externalAlertId) { this.externalAlertId = externalAlertId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    public String getLabels() { return labels; }
    public void setLabels(String labels) { this.labels = labels; }
    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
    public int getNotifiedRepeatCount() { return notifiedRepeatCount; }
    public void setNotifiedRepeatCount(int c) { this.notifiedRepeatCount = c; }
    public int getEscalatedToLevel() { return escalatedToLevel; }
    public void setEscalatedToLevel(int l) { this.escalatedToLevel = l; }
    public LocalDateTime getFirstOccurredAt() { return firstOccurredAt; }
    public void setFirstOccurredAt(LocalDateTime t) { this.firstOccurredAt = t; }
    public LocalDateTime getLastOccurredAt() { return lastOccurredAt; }
    public void setLastOccurredAt(LocalDateTime t) { this.lastOccurredAt = t; }
    public Long getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(Long by) { this.acknowledgedBy = by; }
    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime t) { this.acknowledgedAt = t; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime t) { this.resolvedAt = t; }
}
