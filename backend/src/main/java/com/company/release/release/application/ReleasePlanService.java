package com.company.release.release.application;

import com.company.release.common.exception.BusinessException;
import com.company.release.common.exception.ErrorCode;
import com.company.release.common.redis.DistributedLockService;
import com.company.release.release.domain.model.ReleaseConfigSnapshotEntity;
import com.company.release.release.domain.model.ReleasePlanEntity;
import com.company.release.release.domain.state.ReleaseStatus;
import com.company.release.release.domain.model.ReleaseTaskEntity;
import com.company.release.release.domain.state.IllegalStateTransitionException;
import com.company.release.release.domain.state.ReleaseStateMachine;
import com.company.release.release.repository.ReleaseConfigSnapshotRepository;
import com.company.release.release.repository.ReleasePlanRepository;
import com.company.release.release.repository.ReleaseTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 发布计划应用服务（规范 §8-§18）。
 * 状态流转全部经 ReleaseStateMachine；启动发布加分布式锁；启动即生成配置快照（ADR-008）。
 */
@Service
public class ReleasePlanService {

    private static final Set<String> VALID_ENVIRONMENTS = Set.of("TEST", "PRE", "PROD");

    private final ReleasePlanRepository planRepository;
    private final ReleaseTaskRepository taskRepository;
    private final ReleaseConfigSnapshotRepository snapshotRepository;
    private final DistributedLockService lockService;

    public ReleasePlanService(ReleasePlanRepository planRepository,
                              ReleaseTaskRepository taskRepository,
                              ReleaseConfigSnapshotRepository snapshotRepository,
                              DistributedLockService lockService) {
        this.planRepository = planRepository;
        this.taskRepository = taskRepository;
        this.snapshotRepository = snapshotRepository;
        this.lockService = lockService;
    }

    public record CreatePlanCmd(Long projectId, String name, String versionName,
                                String description, LocalDateTime plannedTime, String environments) {
    }

    @Transactional
    public ReleasePlanEntity create(Long operatorId, CreatePlanCmd cmd) {
        var p = new ReleasePlanEntity();
        p.setProjectId(cmd.projectId());
        p.setName(cmd.name());
        p.setVersionName(cmd.versionName());
        p.setDescription(cmd.description());
        p.setPlannedTime(cmd.plannedTime());
        p.setReleaseOwnerId(operatorId);
        p.setCreatedBy(operatorId);
        if (cmd.environments() != null) {
            validateEnvironmentSelection(cmd.environments());
            p.setEnvironments(cmd.environments());
        }
        return planRepository.save(p);
    }

    /** READY：进入可发布状态，生成配置快照（ADR-008）。 */
    @Transactional
    public void ready(Long operatorId, Long planId) {
        var plan = getOwned(planId, operatorId);
        createSnapshot(plan, operatorId);
        transitInternal(plan, ReleaseStatus.READY);
    }

    /** 状态机驱动的状态流转。 */
    @Transactional
    public void transit(Long operatorId, ReleasePlanEntity plan, String targetStatus) {
        getOwned(plan.getId(), operatorId);
        transitInternal(plan, ReleaseStatus.valueOf(targetStatus));
    }

    /** 系统内部流转（编排器/回调使用）：仅校验状态机合法性，不做操作人校验。 */
    @Transactional
    public void transitSystem(ReleasePlanEntity plan, ReleaseStatus target) {
        var managed = planRepository.findById(plan.getId()).orElse(plan);
        transitInternal(managed, target);
    }

    /** 启动某环境发布（幂等：同环境任务已存在则返回既有任务）。 */
    @Transactional
    public ReleaseTaskEntity startEnvironmentRelease(Long planId, String environmentCode) {
        var plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "release plan not found: " + planId));
        if (!"READY".equals(plan.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "release plan not READY: current=" + plan.getStatus());
        }
        return startEnvironmentReleaseChecked(plan, planId, environmentCode);
    }

    /** 供测试与内部编排复用：不检查 READY（调用方保证）。 */
    public ReleaseTaskEntity startEnvironmentReleaseChecked(ReleasePlanEntity plan, Long planId, String environmentCode) {
        if (!VALID_ENVIRONMENTS.contains(environmentCode)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "invalid environment code: " + environmentCode);
        }
        if (!Set.of(plan.getEnvironments().split(",")).contains(environmentCode)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "environment " + environmentCode + " not selected in this release plan");
        }
        // 幂等：唯一键 (plan, env)
        var existing = taskRepository.findByReleasePlanIdAndEnvironmentCode(planId, environmentCode);
        if (existing.isPresent()) {
            return existing.get();
        }
        // 分布式锁防并发重复创建（agent.md §二十三）
        lockService.lockOrThrow("release:start:" + planId + ":" + environmentCode,
                "plan-" + planId, Duration.ofMinutes(5));
        try {
            var task = new ReleaseTaskEntity();
            task.setReleasePlanId(planId);
            task.setEnvironmentCode(environmentCode);
            task.setStatus("PENDING");
            task.setStartedAt(LocalDateTime.now());
            return taskRepository.save(task);
        } finally {
            lockService.unlock("release:start:" + planId + ":" + environmentCode, "plan-" + planId);
        }
    }

    private void transitInternal(ReleasePlanEntity plan, ReleaseStatus target) {
        try {
            ReleaseStateMachine.transit(ReleaseStatus.valueOf(plan.getStatus()), target);
        } catch (IllegalStateTransitionException e) {
            throw new BusinessException(ErrorCode.CONFLICT, e.getMessage());
        }
        plan.setStatus(target.name());
    }

    private void createSnapshot(ReleasePlanEntity plan, Long operatorId) {
        var snapshot = new ReleaseConfigSnapshotEntity();
        // 快照内容：后续 Phase 接入 ConfigService 后填充完整配置树；
        // 当前记录计划与环境选择基线
        snapshot.setContent("""
                {"planId":%d,"projectId":%d,"environments":"%s","status":"%s"}
                """.formatted(plan.getId(), plan.getProjectId(), plan.getEnvironments(), plan.getStatus()));
        snapshot.setCreatedBy(operatorId);
        plan.setConfigSnapshotId(snapshotRepository.save(snapshot).getId());
    }

    private ReleasePlanEntity getOwned(Long planId, Long operatorId) {
        var plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "release plan not found: " + planId));
        if (!plan.getReleaseOwnerId().equals(operatorId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED,
                    "only release owner can operate this plan");
        }
        return plan;
    }

    private void validateEnvironmentSelection(String csv) {
        for (String env : csv.split(",")) {
            if (!VALID_ENVIRONMENTS.contains(env)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "invalid environment: " + env + ", allowed: TEST/PRE/PROD");
            }
        }
    }

    public ReleaseTaskEntity findTask(Long planId, String environmentCode) {
        return taskRepository.findByReleasePlanIdAndEnvironmentCode(planId, environmentCode).orElse(null);
    }
}
