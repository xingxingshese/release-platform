package com.company.release.release.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 发布计划关联的服务分支信息（规范 §10）。 */
@Entity
@Table(name = "release_plan_service", uniqueConstraints =
@UniqueConstraint(name = "uk_plan_service", columnNames = {"release_plan_id", "service_id"}))
public class PlanServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "release_plan_id", nullable = false)
    private Long releasePlanId;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;

    @Column(name = "source_branch", nullable = false, length = 128)
    private String sourceBranch;

    @Column(name = "target_test_branch", nullable = false, length = 128)
    private String targetTestBranch;

    @Column(name = "release_branch", length = 128)
    private String releaseBranch;

    @Column(name = "commit_id", length = 64)
    private String commitId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReleasePlanId() { return releasePlanId; }
    public void setReleasePlanId(Long releasePlanId) { this.releasePlanId = releasePlanId; }
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public Long getRepositoryId() { return repositoryId; }
    public void setRepositoryId(Long repositoryId) { this.repositoryId = repositoryId; }
    public String getSourceBranch() { return sourceBranch; }
    public void setSourceBranch(String sourceBranch) { this.sourceBranch = sourceBranch; }
    public String getTargetTestBranch() { return targetTestBranch; }
    public void setTargetTestBranch(String targetTestBranch) { this.targetTestBranch = targetTestBranch; }
    public String getReleaseBranch() { return releaseBranch; }
    public void setReleaseBranch(String releaseBranch) { this.releaseBranch = releaseBranch; }
    public String getCommitId() { return commitId; }
    public void setCommitId(String commitId) { this.commitId = commitId; }
}
