package com.company.release.git.application;

import com.company.release.common.exception.BusinessException;
import com.company.release.common.exception.ErrorCode;
import com.company.release.git.api.GitProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Git Merge 编排（规范 §14/§15）：平台执行 merge，冲突返回文件清单进入 WAIT_CONFLICT_RESOLVE。
 */
@Service
public class GitMergeService {

    private final Map<String, GitProvider> providersByType;

    public GitMergeService(Map<String, GitProvider> providersByType) {
        this.providersByType = providersByType;
    }

    public record MergeCmd(String repoUrl, String sourceBranch, String targetBranch) {
    }

    /** 测试环境发布第一步：merge 开发分支 → release_test。providerType 由 git_repository 配置提供。 */
    public GitProvider.MergeResult merge(MergeCmd cmd, String providerType) {
        return mergeWith(cmd, providerType);
    }

    /**
     * 指定 provider 执行 merge。providerType 由 repository 配置决定（禁止硬编码）。
     */
    public GitProvider.MergeResult mergeWith(MergeCmd cmd, String providerType) {
        GitProvider provider = requireProvider(providerType);
        return provider.mergeBranch(cmd.repoUrl(), cmd.sourceBranch(), cmd.targetBranch());
    }

    /** 创建 Release Branch（规范 §17）。 */
    public void createReleaseBranch(GitProvider provider, String repoUrl, String releaseTestBranch, String releaseBranchName) {
        provider.createBranch(repoUrl, releaseTestBranch, releaseBranchName);
    }

    public List<String> branches(String providerType, String repoUrl) {
        return requireProvider(providerType).getBranches(repoUrl);
    }

    private GitProvider requireProvider(String providerType) {
        GitProvider p = providersByType.get(providerType);
        if (p == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "no git provider registered for type: " + providerType);
        }
        return p;
    }
}
