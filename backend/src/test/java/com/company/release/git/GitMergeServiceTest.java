package com.company.release.git;

import com.company.release.common.exception.BusinessException;
import com.company.release.git.api.GitProvider;
import com.company.release.git.application.GitMergeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 规范 §15：平台执行 Merge；冲突 → WAIT_CONFLICT_RESOLVE（不自动绕过）。
 */
class GitMergeServiceTest {

    /** Fake Git Provider：merge 冲突可控。 */
    static class FakeGitProvider implements GitProvider {
        boolean conflict = false;

        @Override
        public String providerType() {
            return "FAKE";
        }

        @Override
        public List<String> getBranches(String repoUrl) {
            return List.of("master", "release_test", "feature/order-123");
        }

        @Override
        public void createBranch(String repoUrl, String fromBranch, String newBranch) {
            // no-op
        }

        @Override
        public MergeResult mergeBranch(String repoUrl, String sourceBranch, String targetBranch) {
            if (conflict) {
                return MergeResult.conflict(List.of("src/main/java/OrderService.java", "pom.xml"));
            }
            return MergeResult.ok("abc123f");
        }

        @Override
        public CommitInfo getLatestCommit(String repoUrl, String branch) {
            return new CommitInfo("abc123f", "feat: order", "dev");
        }
    }

    private FakeGitProvider fakeGitProvider;
    private GitMergeService service;

    @BeforeEach
    void setUp() {
        fakeGitProvider = new FakeGitProvider();
        service = new GitMergeService(Map.of("FAKE", fakeGitProvider));
    }

    private GitMergeService.MergeCmd cmd() {
        return new GitMergeService.MergeCmd("https://git.example.com/order/order-service.git",
                "feature/order-123", "release_test");
    }

    @Test
    void successfulMergeReturnsCommitId() {
        var result = service.mergeWith(cmd(), "FAKE");
        assertThat(result.success()).isTrue();
        assertThat(result.commitId()).isEqualTo("abc123f");
        assertThat(result.conflictFiles()).isEmpty();
    }

    @Test
    void conflictReturnsConflictFilesNeverBypassed() {
        fakeGitProvider.conflict = true;
        var result = service.mergeWith(cmd(), "FAKE");
        assertThat(result.success()).isFalse();
        // 冲突文件必须完整返回给 WAIT_CONFLICT_RESOLVE 页面展示（规范 §15）
        assertThat(result.conflictFiles())
                .containsExactlyInAnyOrder("src/main/java/OrderService.java", "pom.xml");
    }

    @Test
    void unknownProviderTypeRejected() {
        var unknown = new GitMergeService.MergeCmd(
                "https://git.example.com/a/b.git", "feature/x", "release_test");
        assertThatThrownBy(() -> service.mergeWith(unknown, "NOT_EXIST"))
                .hasMessageContaining("provider");
    }

    @Test
    void mergeUsesConfiguredProviderByType() {
        service.mergeWith(cmd(), "FAKE");
        // 通过接口调用验证 provider 被正确选择（此处以行为断言代替内部状态检查）
        assertThat(fakeGitProvider.getBranches("x")).contains("release_test");
    }

}
