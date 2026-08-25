package com.company.release.project.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_member", uniqueConstraints = @UniqueConstraint(
        name = "uk_member", columnNames = {"project_id", "user_id", "role"}))
public class ProjectMemberEntity {

    public static final String PROJECT_OWNER = "PROJECT_OWNER";
    public static final String DEVELOPER = "DEVELOPER";
    public static final String TESTER = "TESTER";
    public static final String PRODUCT = "PRODUCT";
    public static final String RELEASE_OWNER = "RELEASE_OWNER";
    public static final String ALERT_OWNER = "ALERT_OWNER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 项目成员角色（规范 §5），同角色允许多人。 */
    @Column(nullable = false, length = 32)
    private String role;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
