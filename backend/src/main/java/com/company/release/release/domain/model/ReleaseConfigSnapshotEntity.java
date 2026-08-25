package com.company.release.release.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 发布配置快照（ADR-008）：发布启动时复制全部相关配置，后续配置变更不影响本次发布。 */
@Entity
@Table(name = "release_config_snapshot")
public class ReleaseConfigSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(nullable = false, columnDefinition = "JSON")
    private String content;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
