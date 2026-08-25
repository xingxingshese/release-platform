package com.company.release.iam;

import com.company.release.iam.domain.UserEntity;
import com.company.release.iam.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 初始管理员引导：admin/admin123（仅当不存在时创建，生产环境首次登录后必须改密）。
 * 权限点通过 upsert 种子维护并授予 SUPER_ADMIN。
 */
@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private static final String[] PERMISSIONS = {
            "project:manage", "release:edit", "release:test-accept",
            "release:prod:execute", "release:prod:confirm", "config:manage", "alert:ack"
    };

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder, JdbcTemplate jdbc) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        jdbc.update("INSERT INTO role (code, name) VALUES ('SUPER_ADMIN', '超级管理员') " +
                "ON DUPLICATE KEY UPDATE name = VALUES(name)");
        Long roleId = jdbc.queryForObject("SELECT id FROM role WHERE code = 'SUPER_ADMIN'", Long.class);

        for (String code : PERMISSIONS) {
            jdbc.update("INSERT INTO permission (code, name) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE name = VALUES(name)", code, code);
            Long pid = jdbc.queryForObject("SELECT id FROM permission WHERE code = ?", Long.class, code);
            jdbc.update("INSERT IGNORE INTO role_permission (role_id, permission_id) VALUES (?, ?)", roleId, pid);
        }

        if (userRepository.findByUsername("admin").isEmpty()) {
            UserEntity admin = new UserEntity();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setDisplayName("System Administrator");
            admin.setEnabled(true);
            userRepository.save(admin);
            Long uid = userRepository.findByUsername("admin").orElseThrow().getId();
            jdbc.update("INSERT IGNORE INTO user_role (user_id, role_id) VALUES (?, ?)", uid, roleId);
            log.warn("Initial admin created: admin/admin123 — CHANGE PASSWORD IMMEDIATELY IN PRODUCTION");
        }
    }
}
