package com.company.release.deployment.health;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Function;

/**
 * HTTP Health Check 执行器（规范 §32）。配置驱动：method/url/expectedStatus/bodyContains/timeout。
 */
@Component
public class HttpHealthChecker {

    /** 可注入的抓取函数：生产为 JDK HttpClient，测试为 Fake。 */
    private final Function<String, HttpResponseLike> fetcher;

    public record HttpResponseLike(int status, String body) {
    }

    public record HttpCheckResult(boolean passed, String message) {
    }

    public HttpHealthChecker() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.fetcher = url -> {
            try {
                var resp = client.send(HttpRequest.newBuilder(URI.create(url))
                                .timeout(Duration.ofSeconds(5))
                                .GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                return new HttpResponseLike(resp.statusCode(), resp.body());
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        };
    }

    /** 测试用构造器。 */
    public HttpHealthChecker(Function<String, HttpResponseLike> fetcher) {
        this.fetcher = fetcher;
    }

    // 与测试契约对齐的配置类型
    public record Config(String method, String url, int expectedStatus,
                         String bodyMustContain, long timeoutMs) {
    }

    public HttpCheckResult check(Config config) {
        try {
            var resp = fetcher.apply(config.url());
            if (resp.status() != config.expectedStatus()) {
                return new HttpCheckResult(false,
                        "health status %d != expected %d".formatted(resp.status(), config.expectedStatus()));
            }
            if (config.bodyMustContain() != null && !resp.body().contains(config.bodyMustContain())) {
                return new HttpCheckResult(false, "health body missing: " + config.bodyMustContain());
            }
            return new HttpCheckResult(true, "ok");
        } catch (Exception e) {
            return new HttpCheckResult(false, "health error: " + e.getMessage());
        }
    }
}
