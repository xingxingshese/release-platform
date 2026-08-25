package com.company.release.release.application;

import com.company.release.common.exception.BusinessException;
import com.company.release.common.exception.ErrorCode;
import com.company.release.common.observability.ReleaseMetrics;
import com.company.release.common.redis.DistributedLockService;
import com.company.release.iam.PermissionChecker;
import com.company.release.jenkins.api.JenkinsProvider;
import com.company.release.release.domain.model.PlanServiceEntity;
import com.company.release.release.domain.model.ReleasePlanEntity;
import com.company.release.release.domain.model.ReleaseTaskEntity;
import com.company.release.release.repository.PlanServiceRepository;
import com.company.release.release.repository.ReleasePlanRepository;
import com.company.release.release.repository.ReleaseTaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 预发/生产发布编排（spec 009/010，规范 §三）：
 * - PRE：RELEASE_BRANCH_CREATED → PRE_DEPLOYING，使用 release_branch 构建
 * - PROD：独立权限点 release:prod:execute；prodStartAllowed 守卫（并行开关配置化）
 * - 分布式锁防重复触发（§二十三）
 */
@Service
public class PreProdReleaseService {

    private final ReleasePlanRepository planRepository;
    private final ReleaseTaskRepository taskRepository;
    private final PlanServiceRepository planServiceRepository;
    private final JenkinsProvider jenkinsProvider;
    private final PermissionChecker permissionChecker;
    private final DistributedLockService lockService;
    private final ReleaseMetrics metrics;
    private final boolean prodParallelEnabled;

    public PreProdReleaseService(ReleasePlanRepository planRepository,
                                 ReleaseTaskRepository taskRepository,
                                 PlanServiceRepository planServiceRepository,
                                 JenkinsProvider jenkinsProvider,
                                 PermissionChecker permissionChecker,
                                 DistributedLockService lockService,
                                 ReleaseMetrics metrics,
                                 @Value("${release.prod-parallel-enabled:false}") boolean prodParallelEnabled) {
        this.planRepository = planRepository;
        this.taskRepository = taskRepository;
        this.planServiceRepository = planServiceRepository;
        this.jenkinsProvider = jenkinsProvider;
        this.permissionChecker = permissionChecker;
        this.lockService = lockService;
        this.metrics = metrics;
        this.prodParallelEnabled = prodParallelEnabled;
    }

    /** 发起预发发布。 */
    public ReleaseTaskEntity startPreRelease(Long planId, Long operatorId) {
        permissionChecker.checkPermission(operatorId, PermissionChecker.PROD_EXECUTE);
        var plan = requirePlan(planId);
        var lockKey = "release:lock:" + planId + ":PRE";
        if (!lockService.tryLock(lockKey, String.valueOf(operatorId), Duration.ofSeconds(30))) {
            throw new BusinessException(ErrorCode.CONFLICT, "pre release already running for plan " + planId);
        }
        try {
            if (plan.getStatus() == null
                    || !plan.getStatus().equals(com.company.release.release.domain.state.ReleaseStatus.RELEASE_BRANCH_CREATED.name())) {
                throw new BusinessException(ErrorCode.CONFLICT,
                        "PRE requires RELEASE_BRANCH_CREATED, current=" + plan.getStatus());
            }
            plan.setStatus(com.company.release.release.domain.state.ReleaseStatus.PRE_DEPLOYING.name());
            planRepository.save(plan);
            metrics.releaseStarted();
            return trigger(planId, "PRE", releaseBranchOf(planId));
        } finally {
            lockService.unlock(lockKey, String.valueOf(operatorId));
        }
    }

    /** 发起生产发布（独立权限 + 前置状态守卫）。 */
    public ReleaseTaskEntity startProdRelease(Long planId, Long operatorId) {
        permissionChecker.checkPermission(operatorId, PermissionChecker.PROD_EXECUTE);
        var plan = requirePlan(planId);
        var lockKey = "release:lock:" + planId + ":PROD";
        if (!lockService.tryLock(lockKey, String.valueOf(operatorId), Duration.ofSeconds(30))) {
            throw new BusinessException(ErrorCode.CONFLICT, "prod release already running for plan " + planId);
        }
        try {
            var current = com.company.release.release.domain.state.ReleaseStatus.valueOf(plan.getStatus());
            if (!ProdConfirmService.prodStartAllowed(current, prodParallelEnabled)) {
                throw new BusinessException(ErrorCode.CONFLICT,
                        "PROD not allowed from state " + current + " (parallel=" + prodParallelEnabled + ")");
            }
            plan.setStatus(com.company.release.release.domain.state.ReleaseStatus.PROD_DEPLOYING.name());
            planRepository.save(plan);
            metrics.releaseStarted();
            return trigger(planId, "PROD", releaseBranchOf(planId));
        } finally {
            lockService.unlock(lockKey, String.valueOf(operatorId));
        }
    }

    private ReleaseTaskEntity trigger(Long planId, String env, String branch) {
        var task = taskRepository.findByReleasePlanIdAndEnvironmentCode(planId, env)
                .orElseGet(() -> {
                    var t = new ReleaseTaskEntity();
                    t.setReleasePlanId(planId);
                    t.setEnvironmentCode(env);
                    return t;
                });
        long queueId = jenkinsProvider.buildWithParameters("plan-" + planId,
                java.util.Map.of(
                        "BRANCH", branch,
                        "ENV", env,
                        "RELEASE_PLAN_ID", String.valueOf(planId)));
        task.setJenkinsQueueId(queueId);
        Long buildNumber = jenkinsProvider.getBuildNumberFromQueue(queueId);
        if (buildNumber != null) {
            task.setJenkinsBuildNumber(buildNumber);
        }
        task.setStatus("RUNNING");
        task.setStartedAt(java.time.LocalDateTime.now());
        return taskRepository.save(task);
    }

    private ReleasePlanEntity requirePlan(Long planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "release plan not found: " + planId));
    }

    private String releaseBranchOf(Long planId) {
        List<PlanServiceEntity> services = planServiceRepository.findByReleasePlanId(planId);
        if (services.isEmpty() || services.get(0).getReleaseBranch() == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "release branch not created for plan " + planId);
        }
        return services.get(0).getReleaseBranch();
    }
}
