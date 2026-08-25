package com.company.release.git;

import com.company.release.git.api.GitProvider;

import java.util.List;

/** 测试用 Fake Git Provider（agent.md §二十）。 */
public class FakeGitProvider implements GitProvider {

    public boolean conflict = false;
    public int mergeCalls = 0;
    public final List<String> createdBranches = new java.util.ArrayList<>();

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
        createdBranches.add(newBranch);
    }

    @Override
    public MergeResult mergeBranch(String repoUrl, String sourceBranch, String targetBranch) {
        mergeCalls++;
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
