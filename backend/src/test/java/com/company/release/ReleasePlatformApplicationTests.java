package com.company.release;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 0 冒烟测试：Spring 上下文可加载。
 * 外部依赖（MySQL/Redis）在后续 Phase 通过 Testcontainers 做集成测试。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
        })
class ReleasePlatformApplicationTests {

    @org.springframework.boot.test.context.TestConfiguration
    static class MockExternalBeans {
        @org.springframework.context.annotation.Bean
        com.company.release.common.redis.DistributedLockService distributedLockService() {
            return org.mockito.Mockito.mock(com.company.release.common.redis.DistributedLockService.class);
        }

        @org.springframework.context.annotation.Bean
        com.company.release.common.redis.IdempotencyService idempotencyService() {
            return org.mockito.Mockito.mock(com.company.release.common.redis.IdempotencyService.class);
        }

        @org.springframework.context.annotation.Bean
        com.company.release.iam.AdminInitializer adminInitializer() {
            return org.mockito.Mockito.mock(com.company.release.iam.AdminInitializer.class);
        }

        @org.springframework.context.annotation.Bean
        org.springframework.jdbc.core.JdbcTemplate jdbcTemplate() {
            return org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class);
        }

        @org.springframework.context.annotation.Bean
        com.company.release.project.repository.ProjectRepository projectRepository() {
            return org.mockito.Mockito.mock(com.company.release.project.repository.ProjectRepository.class);
        }

        @org.springframework.context.annotation.Bean
        com.company.release.project.repository.ServiceRepository serviceRepository() {
            return org.mockito.Mockito.mock(com.company.release.project.repository.ServiceRepository.class);
        }

        @org.springframework.context.annotation.Bean
        com.company.release.project.repository.ProjectMemberRepository projectMemberRepository() {
            return org.mockito.Mockito.mock(com.company.release.project.repository.ProjectMemberRepository.class);
        }

        @org.springframework.context.annotation.Bean
        com.company.release.requirement.repository.RequirementRepository requirementRepository() {
            return org.mockito.Mockito.mock(com.company.release.requirement.repository.RequirementRepository.class);
        }

        @org.springframework.context.annotation.Bean
        com.company.release.release.repository.ReleasePlanRepository releasePlanRepository() {
            return org.mockito.Mockito.mock(com.company.release.release.repository.ReleasePlanRepository.class);
        }

        @org.springframework.context.annotation.Bean
        com.company.release.release.repository.ReleaseTaskRepository releaseTaskRepository() {
            return org.mockito.Mockito.mock(com.company.release.release.repository.ReleaseTaskRepository.class);
        }

        @org.springframework.context.annotation.Bean
        com.company.release.release.repository.ReleaseConfigSnapshotRepository releaseConfigSnapshotRepository() {
            return org.mockito.Mockito.mock(com.company.release.release.repository.ReleaseConfigSnapshotRepository.class);
        }

        @org.springframework.context.annotation.Bean
        com.company.release.release.repository.PlanServiceRepository planServiceRepository() {
            return org.mockito.Mockito.mock(com.company.release.release.repository.PlanServiceRepository.class);
        }

        @org.springframework.context.annotation.Bean
        com.company.release.jenkins.api.JenkinsProvider jenkinsProvider() {
            return org.mockito.Mockito.mock(com.company.release.jenkins.api.JenkinsProvider.class);
        }

        @org.springframework.context.annotation.Bean
        com.company.release.alert.repository.AlertRepository alertRepository() {
            return org.mockito.Mockito.mock(com.company.release.alert.repository.AlertRepository.class);
        }

        // ---- Phase 11~18 新增模块依赖（JPA 关闭时以 Mock 供上下文加载）----

        @org.springframework.context.annotation.Bean
        com.company.release.admin.ConfigVersionRepository configVersionRepository() {
            return org.mockito.Mockito.mock(com.company.release.admin.ConfigVersionRepository.class);
        }

        @org.springframework.context.annotation.Bean
        com.company.release.audit.OperationLogRepository operationLogRepository() {
            return org.mockito.Mockito.mock(com.company.release.audit.OperationLogRepository.class);
        }

        @org.springframework.context.annotation.Bean
        com.company.release.notification.NotificationRuleRepository notificationRuleRepository() {
            return org.mockito.Mockito.mock(com.company.release.notification.NotificationRuleRepository.class);
        }

        @org.springframework.context.annotation.Bean
        com.company.release.notification.NotificationRecordRepository notificationRecordRepository() {
            return org.mockito.Mockito.mock(com.company.release.notification.NotificationRecordRepository.class);
        }

        @org.springframework.context.annotation.Bean
        com.company.release.deployment.adapter.ReleaseDeploymentNodeRepository releaseDeploymentNodeRepository() {
            return org.mockito.Mockito.mock(com.company.release.deployment.adapter.ReleaseDeploymentNodeRepository.class);
        }

        @org.springframework.context.annotation.Primary
        @org.springframework.context.annotation.Bean
        com.company.release.requirement.application.RequirementService requirementService() {
            return org.mockito.Mockito.mock(com.company.release.requirement.application.RequirementService.class);
        }

        @org.springframework.context.annotation.Primary
        @org.springframework.context.annotation.Bean
        com.company.release.iam.PermissionChecker permissionChecker() {
            return org.mockito.Mockito.mock(com.company.release.iam.PermissionChecker.class);
        }

        @org.springframework.context.annotation.Primary
        @org.springframework.context.annotation.Bean
        com.company.release.iam.auth.AuthService authService() {
            return org.mockito.Mockito.mock(com.company.release.iam.auth.AuthService.class);
        }
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void applicationBeanExists() {
        assertThat(applicationContext.containsBean("releasePlatformApplication")).isTrue();
    }
}
