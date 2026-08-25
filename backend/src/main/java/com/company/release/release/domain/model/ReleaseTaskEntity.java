package com.company.release.release.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 环境级发布任务（规范 §42）：一个发布计划 × 一个环境 = 唯一 ReleaseTask。 */
@Entity
@Table(name = "release_task", uniqueConstraints =
@UniqueConstraint(name = "uk_task_plan_env", columnNames = {"release_plan_id", "environment_code"}))
public class ReleaseTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "release_plan_id", nullable = false)
    private Long releasePlanId;

    @Column(name = "environment_code", nullable = false, length = 32)
    private String environmentCode;

    /** PENDING / RUNNING / SUCCESS / FAILED / TIMEOUT / CANCELLED */
    @Column(nullable = false, length = 48)
    private String status = "PENDING";

    @Column(name = "jenkins_server_id")
    private Long jenkinsServerId;

    @Column(name = "jenkins_job_name")
    private String jenkinsJobName;

    @Column(name = "jenkins_queue_id")
    private Long jenkinsQueueId;

    @Column(name = "jenkins_build_number")
    private Long jenkinsBuildNumber;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReleasePlanId() { return releasePlanId; }
    public void setReleasePlanId(Long releasePlanId) { this.releasePlanId = releasePlanId; }
    public String getEnvironmentCode() { return environmentCode; }
    public void setEnvironmentCode(String environmentCode) { this.environmentCode = environmentCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getJenkinsServerId() { return jenkinsServerId; }
    public void setJenkinsServerId(Long jenkinsServerId) { this.jenkinsServerId = jenkinsServerId; }
    public String getJenkinsJobName() { return jenkinsJobName; }
    public void setJenkinsJobName(String jenkinsJobName) { this.jenkinsJobName = jenkinsJobName; }
    public Long getJenkinsQueueId() { return jenkinsQueueId; }
    public void setJenkinsQueueId(Long jenkinsQueueId) { this.jenkinsQueueId = jenkinsQueueId; }
    public Long getJenkinsBuildNumber() { return jenkinsBuildNumber; }
    public void setJenkinsBuildNumber(Long jenkinsBuildNumber) { this.jenkinsBuildNumber = jenkinsBuildNumber; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
}
