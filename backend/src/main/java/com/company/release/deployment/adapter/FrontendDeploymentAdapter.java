package com.company.release.deployment.adapter;

import com.company.release.deployment.adapter.DeploymentAdapter.DeploymentTarget;
import com.company.release.deployment.health.HttpHealthChecker;
import com.company.release.deployment.health.VersionChecker;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * 前端适配器（规范 §三十四）：dist → OSS/静态服务器 → HTTP Health → Version。
 * 不依赖 K8s；探活失败或 version.json 不一致即判败。
 */
@Component
public class FrontendDeploymentAdapter implements DeploymentAdapter {

    private final Function<String, HttpHealthChecker.HttpResponseLike> httpFetcher;
    private final Function<String, String> bodyFetcher;

    public FrontendDeploymentAdapter() {
        this(FrontendDeploymentAdapter::httpGet, FrontendDeploymentAdapter::httpGetBody);
    }

    public FrontendDeploymentAdapter(Function<String, HttpHealthChecker.HttpResponseLike> httpFetcher,
                                     Function<String, String> bodyFetcher) {
        this.httpFetcher = httpFetcher;
        this.bodyFetcher = bodyFetcher;
    }

    @Override
    public String type() {
        return "FRONTEND";
    }

    @Override
    public VerifyOutcome verify(DeploymentTarget target) {
        var health = new HttpHealthChecker(httpFetcher)
                .check(new HttpHealthChecker.Config("GET", target.healthUrl(), 200, null, 5000));
        if (!health.passed()) {
            return new VerifyOutcome(
                    com.company.release.deployment.verifier.VerifyResult.FAILED,
                    null, null, null, null, null,
                    false, target.expectedVersion(), null, null,
                    "health: " + health.message());
        }
        var version = new VersionChecker(bodyFetcher)
                .check(new VersionChecker.VersionCheckConfig(target.versionUrl(), target.expectedVersion()));
        if (!version.passed()) {
            return new VerifyOutcome(
                    com.company.release.deployment.verifier.VerifyResult.VERSION_CHECK_FAILED,
                    null, null, null, null, null,
                    true, target.expectedVersion(),
                    version.actual(), false,
                    "version: " + version.message());
        }
        return new VerifyOutcome(
                com.company.release.deployment.verifier.VerifyResult.SUCCESS,
                null, null, null, null, null,
                true, target.expectedVersion(), target.expectedVersion(), true,
                "frontend verify ok");
    }

    private static HttpHealthChecker.HttpResponseLike httpGet(String url) {
        return http(url);
    }

    private static String httpGetBody(String url) {
        return httpBody(url);
    }

    private static HttpHealthChecker.HttpResponseLike http(String url) {
        try {
            var resp = send(url);
            return new HttpHealthChecker.HttpResponseLike(resp.statusCode(), resp.body());
        } catch (Exception e) {
            throw new IllegalStateException("http get failed: " + url + ", " + e.getMessage(), e);
        }
    }

    private static String httpBody(String url) {
        try {
            return send(url).body();
        } catch (Exception e) {
            throw new IllegalStateException("http get failed: " + url + ", " + e.getMessage(), e);
        }
    }

    private static java.net.http.HttpResponse<String> send(String url) throws Exception {
        var client = java.net.http.HttpClient.newHttpClient();
        return client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(5)).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
    }
}
