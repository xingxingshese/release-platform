package com.company.release.requirement.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "requirement")
public class RequirementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private String title;

    @Column(length = 1024)
    private String description;

    /** MANUAL / YUNXIAO / JIRA / TAPD / OTHER */
    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(name = "external_id", length = 128)
    private String externalId;

    @Column(name = "external_url", length = 512)
    private String externalUrl;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(length = 16)
    private String priority;

    @Column(nullable = false, length = 32)
    private String status = "OPEN";

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getExternalUrl() { return externalUrl; }
    public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
