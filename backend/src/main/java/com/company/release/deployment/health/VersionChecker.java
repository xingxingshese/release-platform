package com.company.release.deployment.health;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * Version Check 执行器（规范 §33）：GET version.json / /api/system/version，
 * 校验运行版本 == 本次发布版本，不一致 → VERSION_CHECK_FAILED。
 */
@Component
public class VersionChecker {

    private final Function<String, String> fetcher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record VersionCheckConfig(String url, String expectedVersion) {
    }

    public record VersionCheckResult(boolean passed, String actual, String message) {
    }

    /** 生产构造器：HTTP 抓取。 */
    public VersionChecker() {
        this(url -> {
            try {
                var client = HttpClientHolder.CLIENT;
                var resp = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                                .timeout(Duration5s.get())
                                .GET().build(),
                        java.net.http.HttpResponse.BodyHandlers.ofString());
                return resp.body();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }

    /** 测试构造器。 */
    public VersionChecker(Function<String, String> fetcher) {
        this.fetcher = fetcher;
    }

    public VersionCheckResult check(VersionCheckConfig config) {
        try {
            String body = fetcher.apply(config.url());
            JsonNode node = objectMapper.readTree(body);
            String actual = node.path("version").asText(null);
            boolean pass = config.expectedVersion() != null && config.expectedVersion().equals(actual);
            return new VersionCheckResult(pass, actual,
                    pass ? "ok" : "version mismatch: expected=%s actual=%s"
                            .formatted(config.expectedVersion(), actual));
        } catch (Exception e) {
            return new VersionCheckResult(false, null, "version check error: " + e.getMessage());
        }
    }

    private static final class HttpClientHolder {
        static final java.net.http.HttpClient CLIENT =
                java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
    }

    private static final class Duration5s {
        static java.time.Duration get() {
            return java.time.Duration.ofSeconds(5);
        }
    }
}
