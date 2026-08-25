package com.company.release.release.application;

import com.company.release.common.exception.BusinessException;
import com.company.release.common.exception.ErrorCode;
import com.company.release.iam.PermissionChecker;
import com.company.release.release.domain.model.ReleasePlanEntity;
import com.company.release.release.domain.state.ReleaseStatus;
import com.company.release.release.repository.ReleasePlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测试验收（规范 §16）：WAIT_TEST_ACCEPT → TEST_ACCEPTED / TEST_REJECTED。
 * 仅 TEST_ACCEPTED 允许创建 Release Branch。接口幂等：重复验收不改变状态。
 */
@Service
public class AcceptanceService {

    private final ReleasePlanRepository planRepository;
    private final PermissionChecker permissionChecker;

    public AcceptanceService(ReleasePlanRepository planRepository, PermissionChecker permissionChecker) {
        this.planRepository = planRepository;
        this.permissionChecker = permissionChecker;
    }

    @Transactional
    public void accept(Long operatorId, Long planId) {
        decide(operatorId, planId, ReleaseStatus.TEST_ACCEPTED);
    }

    @Transactional
    public void reject(Long operatorId, Long planId) {
        decide(operatorId, planId, ReleaseStatus.TEST_REJECTED);
    }

    private void decide(Long operatorId, Long planId, ReleaseStatus decision) {
        permissionChecker.checkPermission(operatorId, PermissionChecker.TEST_ACCEPT);
        var plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "release plan not found: " + planId));
        var current = ReleaseStatus.valueOf(plan.getStatus());
        // 幂等：重复同一决定直接返回成功（ADR-010）
        if (current == decision && decision == ReleaseStatus.TEST_ACCEPTED) {
            return;
        }
        try {
            com.company.release.release.domain.state.ReleaseStateMachine.transit(current, decision);
        } catch (com.company.release.release.domain.state.IllegalStateTransitionException e) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "acceptance only allowed in WAIT_TEST_ACCEPT, current=" + current);
        }
        plan.setStatus(decision.name());
        planRepository.save(plan);
    }
}
