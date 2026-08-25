package com.company.release.deployment.adapter;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 发布部署节点逐实例判定明细（表 V10，spec 011/012）。 */
@Entity
@Table(name = "release_deployment_node")
public class ReleaseDeploymentNodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "release_task_id", nullable = false)
    private Long releaseTaskId;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "node_name", nullable = false)
    private String nodeName;

    /** K8S / FRONTEND */
    @Column(name = "deployment_type", nullable = false)
    private String deploymentType;

    private Integer replicaDesired;
    private Integer replicaUpdated;
    private Integer replicaReady;
    private Integer replicaAvailable;
    private Integer replicaUnavailable;

    private Boolean healthPassed;
    @Column(name = "version_expected")
    private String versionExpected;
    @Column(name = "version_actual")
    private String versionActual;
    private Boolean versionPassed;

    @Column(nullable = false)
    private String result;

    private String message;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Long getReleaseTaskId() { return releaseTaskId; }
    public void setReleaseTaskId(Long v) { this.releaseTaskId = v; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String v) { this.serviceName = v; }
    public String getNodeName() { return nodeName; }
    public void setNodeName(String v) { this.nodeName = v; }
    public String getDeploymentType() { return deploymentType; }
    public void setDeploymentType(String v) { this.deploymentType = v; }
    public Integer getReplicaDesired() { return replicaDesired; }
    public void setReplicaDesired(Integer v) { this.replicaDesired = v; }
    public Integer getReplicaUpdated() { return replicaUpdated; }
    public void setReplicaUpdated(Integer v) { this.replicaUpdated = v; }
    public Integer getReplicaReady() { return replicaReady; }
    public void setReplicaReady(Integer v) { this.replicaReady = v; }
    public Integer getReplicaAvailable() { return replicaAvailable; }
    public void setReplicaAvailable(Integer v) { this.replicaAvailable = v; }
    public Integer getReplicaUnavailable() { return replicaUnavailable; }
    public void setReplicaUnavailable(Integer v) { this.replicaUnavailable = v; }
    public Boolean getHealthPassed() { return healthPassed; }
    public void setHealthPassed(Boolean v) { this.healthPassed = v; }
    public String getVersionExpected() { return versionExpected; }
    public void setVersionExpected(String v) { this.versionExpected = v; }
    public String getVersionActual() { return versionActual; }
    public void setVersionActual(String v) { this.versionActual = v; }
    public Boolean getVersionPassed() { return versionPassed; }
    public void setVersionPassed(Boolean v) { this.versionPassed = v; }
    public String getResult() { return result; }
    public void setResult(String v) { this.result = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
