package com.company.release.deployment.adapter;

import com.company.release.deployment.health.HttpHealthChecker;
import com.company.release.deployment.health.VersionChecker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 规范 §34：前端发布不依赖 K8s Pod；HTTP Health + Version Check 统一为 FrontendVerifier。
 */
class FrontendVerifierTest {

    @Test
    void successWhenHealthAndVersionPass() {
        var verifier = new FrontendVerifier(
                url -> new HttpHealthChecker.HttpResponseLike(200, "ok"),
                url -> "{\"version\":\"2026.08.24\"}");
        var result = verifier.verify(new FrontendVerifier.FrontendDeployment(
                "https://cdn.example.com/", "https://cdn.example.com/version.json", "2026.08.24"));
        assertThat(result.passed()).isTrue();
    }

    @Test
    void failsWhenVersionMismatch() {
        var verifier = new FrontendVerifier(
                url -> new HttpHealthChecker.HttpResponseLike(200, "ok"),
                url -> "{\"version\":\"old\"}");
        var result = verifier.verify(new FrontendVerifier.FrontendDeployment(
                "https://cdn.example.com/", "https://cdn.example.com/version.json", "2026.08.24"));
        assertThat(result.passed()).isFalse();
    }

    @Test
    void failsWhenHealthDown() {
        var verifier = new FrontendVerifier(
                url -> new HttpHealthChecker.HttpResponseLike(500, "err"),
                url -> "{\"version\":\"2026.08.24\"}");
        var result = verifier.verify(new FrontendVerifier.FrontendDeployment(
                "https://cdn.example.com/", "https://cdn.example.com/version.json", "2026.08.24"));
        assertThat(result.passed()).isFalse();
    }
}
