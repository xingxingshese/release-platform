package com.company.release.iam.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色权限查询：user → user_role → role_permission → permission.code。
 */
@Repository
public class PermissionQueryDao {

    private final JdbcTemplate jdbc;

    public PermissionQueryDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<String> findPermissionCodesByUserId(Long userId) {
        return jdbc.queryForList("""
                SELECT DISTINCT p.code
                FROM app_user u
                JOIN user_role ur ON ur.user_id = u.id
                JOIN role_permission rp ON rp.role_id = ur.role_id
                JOIN permission p ON p.id = rp.permission_id
                WHERE u.id = ?
                """, String.class, userId);
    }
}
