package com.company.release.notification;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 通知发送记录（表 V8__alert.sql 的 notification_record）。 */
@Entity
@Table(name = "notification_record")
public class NotificationRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String receiver;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text", nullable = false)
    private String content;

    @Column(name = "related_type")
    private String relatedType;

    @Column(name = "related_id")
    private String relatedId;

    @Column(nullable = false)
    private boolean success = true;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getChannel() { return channel; }
    public void setChannel(String v) { this.channel = v; }
    public String getReceiver() { return receiver; }
    public void setReceiver(String v) { this.receiver = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public String getRelatedType() { return relatedType; }
    public void setRelatedType(String v) { this.relatedType = v; }
    public String getRelatedId() { return relatedId; }
    public void setRelatedId(String v) { this.relatedId = v; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean v) { this.success = v; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }
}
