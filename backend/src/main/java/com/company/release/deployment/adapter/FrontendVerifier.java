package com.company.release.deployment.adapter;

import com.company.release.deployment.health.HttpHealthChecker;
import com.company.release.deployment.health.VersionChecker;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * 前端部署验证器（规范 §34）：dist → OSS/静态服务器 → HTTP Health Check → Version Check。
 * 不依赖 K8s Pod 判断。
 */
@Component
public class FrontendVerifier {

    private final Function<String, HttpHealthChecker.HttpResponseLike> httpFetcher;
    private final Function<String, String> bodyFetcher;

    public record FrontendDeployment(String healthUrl, String versionUrl, String expectedVersion) {
    }

    public record VerifyResult(boolean passed, String message) {
    }

    public FrontendVerifier() {
        this(FrontendVerifier::httpGet, FrontendVerifier::httpGetBody);
    }
    private static HttpHealthChecker.HttpResponseLike httpGet(String url) {
        try {
            var client = java.net.http.HttpClient.newHttpClient();
            var resp = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                            .timeout(java.time.Duration.ofSeconds(5)).GET().build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            return new HttpHealthChecker.HttpResponseLike(resp.statusCode(), resp.body());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static String httpGetBody(String url) {
        try {
            var client = java.net.http.HttpClient.newHttpClient();
            var resp = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                            .timeout(java.time.Duration.ofSeconds(5)).GET().build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            return resp.body();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public FrontendVerifier(Function<String, HttpHealthChecker.HttpResponseLike> httpFetcher,
                            Function<String, String> bodyFetcher) {
        this.httpFetcher = httpFetcher;
        this.bodyFetcher = bodyFetcher;
    }

    public VerifyResult verify(FrontendDeployment d) {
        var health = new HttpHealthChecker(url -> httpFetcher.apply(url))
                .check(new HttpHealthChecker.Config("GET", d.healthUrl(), 200, null, 5000));
        if (!health.passed()) {
            return new VerifyResult(false, "health: " + health.message());
        }
        var version = new VersionChecker(bodyFetcher)
                .check(new VersionChecker.VersionCheckConfig(d.versionUrl(), d.expectedVersion()));
        if (!version.passed()) {
            return new VerifyResult(false, "version: " + version.message());
        }
        return new VerifyResult(true, "ok");
    }
}
