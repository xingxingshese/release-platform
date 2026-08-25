package com.company.release.deployment.health;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 规范 §32/§33：Health Check 与 Version Check 配置化执行器。
 * HTTP 抓取函数注入，便于测试与真实实现替换。
 */
class HealthAndVersionCheckTest {

    // ---- Health Check ----

    @Test
    void healthPassesOnExpectedStatus() {
        var config = new HttpHealthChecker.Config("GET", "http://app/actuator/health", 200, "UP", 2000);
        var checker = new HttpHealthChecker(url -> new HttpHealthChecker.HttpResponseLike(200, "{\"status\":\"UP\"}"));
        assertThat(checker.check(config).passed()).isTrue();
    }

    @Test
    void healthFailsOnWrongStatus() {
        var config = new HttpHealthChecker.Config("GET", "http://app/actuator/health", 200, null, 2000);
        var checker = new HttpHealthChecker(url -> new HttpHealthChecker.HttpResponseLike(503, "unavailable"));
        assertThat(checker.check(config).passed()).isFalse();
    }

    @Test
    void healthFailsWhenExpectedBodyMissing() {
        var config = new HttpHealthChecker.Config("GET", "http://app/actuator/health", 200, "DOWN_EXPECTED_NEVER", 2000);
        var checker = new HttpHealthChecker(url -> new HttpHealthChecker.HttpResponseLike(200, "{\"status\":\"UP\"}"));
        assertThat(checker.check(config).passed()).isFalse();
    }

    @Test
    void healthFailsOnException() {
        var config = new HttpHealthChecker.Config("GET", "http://app/health", 200, null, 100);
        var checker = new HttpHealthChecker(url -> {
            throw new RuntimeException("connect timeout");
        });
        assertThat(checker.check(config).passed()).isFalse();
    }

    // ---- Version Check ----

    private static final String VERSION_JSON = """
            {"version":"2026.08.24.10086","commit":"a83fd29"}
            """;

    @Test
    void versionPassesWhenMatches() {
        var result = new VersionChecker(url -> VERSION_JSON)
                .check(new VersionChecker.VersionCheckConfig("http://app/version.json", "2026.08.24.10086"));
        assertThat(result.passed()).isTrue();
    }

    @Test
    void versionMismatchRejected() {
        var result = new VersionChecker(url -> VERSION_JSON)
                .check(new VersionChecker.VersionCheckConfig("http://app/version.json", "2026.01.01.00001"));
        assertThat(result.passed()).isFalse();
    }

    @Test
    void versionFetchFailureRejected() {
        var result = new VersionChecker((java.util.function.Function<String, String>) url -> {
            throw new RuntimeException("404");
        }).check(new VersionChecker.VersionCheckConfig("http://app/version.json", "2026.08.24.10086"));
        assertThat(result.passed()).isFalse();
    }
}
