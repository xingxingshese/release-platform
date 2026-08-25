package com.company.release.admin;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 配置版本（表 V1__base_tables.sql，ADR-008）：任何影响发布的配置变更产生新版本。 */
@Entity
@Table(name = "config_version")
public class ConfigVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_type", nullable = false)
    private String configType;

    @Column(name = "config_key", nullable = false)
    private String configKey;

    @Column(nullable = false)
    private int version;

    @Column(columnDefinition = "json", nullable = false)
    private String content;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @Column(name = "change_reason")
    private String changeReason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getConfigType() { return configType; }
    public void setConfigType(String v) { this.configType = v; }
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String v) { this.configKey = v; }
    public int getVersion() { return version; }
    public void setVersion(int v) { this.version = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public Long getChangedBy() { return changedBy; }
    public void setChangedBy(Long v) { this.changedBy = v; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String v) { this.changeReason = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
