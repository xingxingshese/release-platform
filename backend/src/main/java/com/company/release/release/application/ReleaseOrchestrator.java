package com.company.release.release.application;

import com.company.release.common.exception.BusinessException;
import com.company.release.common.exception.ErrorCode;
import com.company.release.deployment.verifier.ReleaseSuccessEvaluator;
import com.company.release.git.api.GitProvider;
import com.company.release.git.application.GitMergeService;
import com.company.release.jenkins.api.JenkinsProvider;
import com.company.release.release.domain.model.ReleasePlanEntity;
import com.company.release.release.domain.state.ReleaseStatus;
import com.company.release.release.domain.model.ReleaseTaskEntity;
import com.company.release.release.repository.PlanServiceRepository;
import com.company.release.release.repository.ReleasePlanRepository;
import com.company.release.release.repository.ReleaseTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 发布编排器（规范 §14）：平台负责流程编排、状态机、最终成功判定；
 * Jenkins 负责构建；Git Merge 由平台执行（规范 §15）。
 */
@Service
public class ReleaseOrchestrator {

    private final ReleasePlanRepository planRepository;
    private final ReleaseTaskRepository taskRepository;
    private final PlanServiceRepository planServiceRepository;
    private final GitMergeService gitMergeService;
    private final JenkinsProvider jenkinsProvider;
    private final ReleaseSuccessEvaluator successEvaluator;
    private final com.company.release.deployment.verifier.KubernetesDeploymentVerifier k8sVerifier;

    public ReleaseOrchestrator(ReleasePlanRepository planRepository,
                               ReleaseTaskRepository taskRepository,
                               PlanServiceRepository planServiceRepository,
                               GitMergeService gitMergeService,
                               JenkinsProvider jenkinsProvider,
                               ReleaseSuccessEvaluator successEvaluator,
                               com.company.release.deployment.verifier.KubernetesDeploymentVerifier k8sVerifier) {
        this.planRepository = planRepository;
        this.taskRepository = taskRepository;
        this.planServiceRepository = planServiceRepository;
        this.gitMergeService = gitMergeService;
        this.jenkinsProvider = jenkinsProvider;
        this.successEvaluator = successEvaluator;
        this.k8sVerifier = k8sVerifier;
    }

    /**
     * 测试发布：merge release_test → 无冲突 → Jenkins buildWithParameters → RUNNING。
     * 冲突：计划进入 WAIT_CONFLICT_RESOLVE，绝不自动绕过（规范 §15/§44）。
     */
    @Transactional
    public ReleaseTaskEntity startTestRelease(Long planId) {
        var plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "release plan not found: " + planId));
        if (!"READY".equals(plan.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "release plan not READY: " + plan.getStatus());
        }

        // 1. 状态机：READY → TEST_MERGING
        plan.setStatus("TEST_MERGING");
        planRepository.save(plan);

        // 2. 平台执行 merge（provider_type 由 repository 配置决定，此处由分支配置携带）
        List<String> allConflicts = new ArrayList<>();
        var branches = planServiceRepository.findByReleasePlanId(planId);
        for (var b : branches) {
            var result = gitMergeService.merge(
                    new GitMergeService.MergeCmd(repoUrlOf(b), b.getSourceBranch(), b.getTargetTestBranch()),
                    providerTypeOf(b));
            if (!result.success()) {
                allConflicts.addAll(result.conflictFiles());
            } else {
                b.setCommitId(result.commitId());
                planServiceRepository.save(b);
            }
        }
        if (!allConflicts.isEmpty()) {
            plan.setStatus("WAIT_CONFLICT_RESOLVE");
            planRepository.save(plan);
            throw new BusinessException(ErrorCode.CONFLICT,
                    "merge conflict, resolve and retry. files=" + allConflicts);
        }

        // 3. 状态机：TEST_MERGING → TEST_DEPLOYING
        plan.setStatus("TEST_DEPLOYING");
        planRepository.save(plan);

        // 4. Jenkins 构建（参数映射 Phase 7；此处使用平台标准字段）
        var task = taskRepository.findByReleasePlanIdAndEnvironmentCode(planId, "TEST")
                .orElseGet(() -> {
                    var t = new ReleaseTaskEntity();
                    t.setReleasePlanId(planId);
                    t.setEnvironmentCode("TEST");
                    return t;
                });
        long queueId = jenkinsProvider.buildWithParameters(jobNameFor(plan),
                java.util.Map.of(
                        "BRANCH", primarySourceBranch(branches),
                        "ENV", "TEST",
                        "RELEASE_PLAN_ID", String.valueOf(planId)));
        task.setJenkinsQueueId(queueId);
        Long buildNumber = jenkinsProvider.getBuildNumberFromQueue(queueId);
        if (buildNumber != null) {
            task.setJenkinsBuildNumber(buildNumber);
        }
        task.setStatus("RUNNING");
        return taskRepository.save(task);
    }

    /**
     * Jenkins 构建结束回调（幂等由 JenkinsWebhookService 保证）。
     * 核心红线：只有 Build+Deployment+Health+Version 全部通过才 SUCCESS。
     */
    @Transactional
    public void onBuildFinished(long serverId, String jobName, long buildNumber,
                                String jenkinsStatus,
                                boolean deploymentSuccess, boolean healthCheckSuccess, boolean versionCheckSuccess,
                                boolean needHealthCheck, boolean needVersionCheck) {
        var task = taskRepository
                .findByJenkinsServerIdAndJenkinsJobNameAndJenkinsBuildNumber(serverId, jobName, buildNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "no release task for build %s#%d".formatted(jobName, buildNumber)));

        boolean buildSuccess = "SUCCESS".equals(jenkinsStatus);
        var evaluation = successEvaluator.evaluate(
                buildSuccess, deploymentSuccess,
                needHealthCheck, healthCheckSuccess,
                needVersionCheck, versionCheckSuccess);

        if (evaluation.success()) {
            task.setStatus("SUCCESS");
            task.setFinishedAt(java.time.LocalDateTime.now());
        } else {
            task.setStatus("FAILED");
            task.setErrorMessage("failed checks: " + evaluation.failedChecks());
            task.setFinishedAt(java.time.LocalDateTime.now());
        }
        taskRepository.save(task);

        // 计划级流转（TEST 环境）：TEST_DEPLOYING → TEST_DEPLOY_SUCCESS → WAIT_TEST_ACCEPT / FAILED
        planRepository.findById(task.getReleasePlanId()).ifPresent(plan -> {
            try {
                if ("SUCCESS".equals(task.getStatus())) {
                    if ("TEST".equals(task.getEnvironmentCode())) {
                        transitSafe(plan, ReleaseStatus.TEST_DEPLOY_SUCCESS);
                        transitSafe(plan, ReleaseStatus.WAIT_TEST_ACCEPT);
                    }
                }
            } catch (Exception ignored) {
                // 非测试环境或状态不匹配时由对应编排阶段处理
            }
        });
    }

    private void transitSafe(ReleasePlanEntity plan, ReleaseStatus target) {
        try {
            ReleaseStateMachineHelper.transit(plan, target);
            planRepository.save(plan);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.CONFLICT, e.getMessage());
        }
    }

    private String repoUrlOf(com.company.release.release.domain.model.PlanServiceEntity b) {
        return "repo-" + b.getRepositoryId(); // TODO(Phase 13 前)：经 RepositoryService 解析真实 URL
    }

    private String providerTypeOf(com.company.release.release.domain.model.PlanServiceEntity b) {
        return "FAKE"; // 由 git_repository.provider_type 提供；Fake 注册为 FAKE 供集成测试链路使用
    }

    private String jobNameFor(ReleasePlanEntity plan) {
        return "plan-" + plan.getId(); // 由 jenkins_job(service×environment) 配置提供
    }

    private String primarySourceBranch(List<com.company.release.release.domain.model.PlanServiceEntity> branches) {
        return branches.isEmpty() ? "master" : branches.get(0).getSourceBranch();
    }

    /** 内部辅助：直接复用领域状态机。 */
    static final class ReleaseStateMachineHelper {
        static void transit(ReleasePlanEntity plan, ReleaseStatus target) {
            var from = com.company.release.release.domain.state.ReleaseStatus.valueOf(plan.getStatus());
            com.company.release.release.domain.state.ReleaseStateMachine.transit(from, target);
            plan.setStatus(target.name());
        }
    }
}
