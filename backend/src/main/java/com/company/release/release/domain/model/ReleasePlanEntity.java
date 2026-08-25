package com.company.release.release.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 发布计划（规范 §8）。状态流转只允许经 ReleaseStateMachine。 */
@Entity
@Table(name = "release_plan")
public class ReleasePlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "version_name", length = 64)
    private String versionName;

    @Column(length = 1024)
    private String description;

    @Column(name = "release_owner_id", nullable = false)
    private Long releaseOwnerId;

    @Column(name = "planned_time")
    private LocalDateTime plannedTime;

    /** 见 ReleaseStatus；以字符串存储避免 JPA 枚举迁移问题。 */
    @Column(nullable = false, length = 48)
    private String status = "DRAFT";

    /** TEST / PRE / PROD 组合（规范 §18：只发 PRE / 只发 PROD / PRE+PROD）。 */
    @Column(name = "environments", nullable = false, length = 128)
    private String environments = "TEST";

    @Column(name = "config_snapshot_id")
    private Long configSnapshotId;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVersionName() { return versionName; }
    public void setVersionName(String versionName) { this.versionName = versionName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getReleaseOwnerId() { return releaseOwnerId; }
    public void setReleaseOwnerId(Long releaseOwnerId) { this.releaseOwnerId = releaseOwnerId; }
    public LocalDateTime getPlannedTime() { return plannedTime; }
    public void setPlannedTime(LocalDateTime plannedTime) { this.plannedTime = plannedTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEnvironments() { return environments; }
    public void setEnvironments(String environments) { this.environments = environments; }
    public Long getConfigSnapshotId() { return configSnapshotId; }
    public void setConfigSnapshotId(Long configSnapshotId) { this.configSnapshotId = configSnapshotId; }
    public long getVersion() { return version; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
