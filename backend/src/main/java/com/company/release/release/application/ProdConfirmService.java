package com.company.release.release.application;

import com.company.release.common.exception.BusinessException;
import com.company.release.common.exception.ErrorCode;
import com.company.release.iam.PermissionChecker;
import com.company.release.release.domain.state.ReleaseStatus;
import com.company.release.release.repository.ReleasePlanRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 生产发布守卫与确认（规范 §18/§39）：
 * - 生产发布独立权限点 release:prod:execute
 * - PRE→PROD 是否并行由配置决定（默认 PRE 成功后才能 PROD）
 * - 生产确认：WAIT_PROD_CONFIRM → COMPLETED，权限点 release:prod:confirm，重复确认幂等
 */
@Service
public class ProdConfirmService {

    private final ReleasePlanRepository planRepository;
    private final PermissionChecker permissionChecker;
    private final boolean prodParallelEnabled;

    public ProdConfirmService(ReleasePlanRepository planRepository,
                              PermissionChecker permissionChecker,
                              @Value("${release.prod-parallel-enabled:false}") boolean prodParallelEnabled) {
        this.planRepository = planRepository;
        this.permissionChecker = permissionChecker;
        this.prodParallelEnabled = prodParallelEnabled;
    }

    /** 生产发布前置状态判定（静态便于测试）。 */
    public static boolean prodStartAllowed(ReleaseStatus current, boolean parallelEnabled) {
        if (parallelEnabled) {
            return current == ReleaseStatus.PRE_DEPLOY_SUCCESS || current == ReleaseStatus.READY
                    || current == ReleaseStatus.RELEASE_BRANCH_CREATED;
        }
        return current == ReleaseStatus.PRE_DEPLOY_SUCCESS;
    }

    /** 供编排器调用的生产权限校验。 */
    public static void checkProdPermission(Long operatorId, PermissionChecker checker) {
        checker.checkPermission(operatorId, PermissionChecker.PROD_EXECUTE);
    }

    /** 生产确认：仅 WAIT_PROD_CONFIRM 可确认；已 COMPLETED 重复确认幂等。 */
    @Transactional
    public void confirm(Long operatorId, Long planId) {
        permissionChecker.checkPermission(operatorId, PermissionChecker.PROD_CONFIRM);
        var plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "release plan not found: " + planId));
        var current = ReleaseStatus.valueOf(plan.getStatus());
        if (current == ReleaseStatus.COMPLETED) {
            return; // 幂等
        }
        try {
            ReleaseStateMachineBridge.transit(current, ReleaseStatus.COMPLETED);
        } catch (com.company.release.release.domain.state.IllegalStateTransitionException e) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "confirm only allowed in WAIT_PROD_CONFIRM, current=" + current);
        }
        plan.setStatus(ReleaseStatus.COMPLETED.name());
        planRepository.save(plan);
    }

    /** 内部桥接：直接使用领域状态机。 */
    private static final class ReleaseStateMachineBridge {
        static void transit(ReleaseStatus from, ReleaseStatus to) {
            com.company.release.release.domain.state.ReleaseStateMachine.transit(from, to);
        }
    }

    /** 测试辅助：字符串转状态。 */
    public static ReleaseStatus planState(String s) {
        return ReleaseStatus.valueOf(s);
    }
}
