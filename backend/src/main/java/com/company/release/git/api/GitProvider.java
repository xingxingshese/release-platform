package com.company.release.git.api;

import java.util.List;

/**
 * Git Provider 统一接口（规范 §7）。实现：GitLab/GitHub/Gitee/Codeup/Custom；测试用 Fake。
 * 平台执行 Merge（规范 §15），不由 Jenkins 负责。
 */
public interface GitProvider {

    /** 对应 provider_type。 */
    String providerType();

    record MergeResult(boolean success, String commitId, List<String> conflictFiles, String message) {

        public static MergeResult ok(String commitId) {
            return new MergeResult(true, commitId, List.of(), null);
        }

        public static MergeResult conflict(List<String> files) {
            return new MergeResult(false, null, files, "merge conflict");
        }
    }

    record CommitInfo(String sha, String message, String author) {
    }

    List<String> getBranches(String repoUrl);

    void createBranch(String repoUrl, String fromBranch, String newBranch);

    /** 将 sourceBranch 合入 targetBranch；冲突时返回冲突文件列表，禁止自动绕过。 */
    MergeResult mergeBranch(String repoUrl, String sourceBranch, String targetBranch);

    CommitInfo getLatestCommit(String repoUrl, String branch);
}
