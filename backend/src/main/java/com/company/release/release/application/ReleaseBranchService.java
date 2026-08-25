package com.company.release.release.application;

import com.company.release.common.exception.BusinessException;
import com.company.release.common.exception.ErrorCode;
import com.company.release.iam.PermissionChecker;
import com.company.release.release.domain.model.ReleasePlanEntity;
import com.company.release.release.domain.state.ReleaseStatus;
import com.company.release.release.repository.PlanServiceRepository;
import com.company.release.release.repository.ReleasePlanRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Release Branch 创建（规范 §17）：
 * release_{yyyyMMdd}_{releasePlanId}（模板配置化），基于 release_test，仅验收通过后可创建。
 */
@Service
public class ReleaseBranchService {

    private final ReleasePlanRepository planRepository;
    private final PlanServiceRepository planServiceRepository;
    private final PermissionChecker permissionChecker;
    private final String branchTemplate;

    public ReleaseBranchService(ReleasePlanRepository planRepository,
                                PlanServiceRepository planServiceRepository,
                                PermissionChecker permissionChecker,
                                @Value("${release.branch-template:release_{yyyyMMdd}_{releasePlanId}}")
                                String branchTemplate) {
        this.planRepository = planRepository;
        this.planServiceRepository = planServiceRepository;
        this.permissionChecker = permissionChecker;
        this.branchTemplate = branchTemplate;
    }

    /** 幂等：已创建过（release_branch 已保存）直接返回。 */
    public void create(Long operatorId, Long planId) {
        permissionChecker.checkPermission(operatorId, PermissionChecker.RELEASE_EDIT);
        var plan = requirePlan(planId);
        var branches = planServiceRepository.findByReleasePlanId(planId);
        if (!branches.isEmpty() && branches.stream().allMatch(b -> b.getReleaseBranch() != null)) {
            return; // 幂等
        }
        String branchName = branchName(plan);
        branches.forEach(b -> b.setReleaseBranch(branchName));
        planServiceRepository.saveAll(branches);
    }

    public String branchName(ReleasePlanEntity plan) {
        return branchTemplate
                .replace("{yyyyMMdd}", java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE))
                .replace("{releasePlanId}", String.valueOf(plan.getId()));
    }

    private ReleasePlanEntity requirePlan(Long planId) {
        var plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "release plan not found: " + planId));
        if (ReleaseStatus.valueOf(plan.getStatus()) != ReleaseStatus.TEST_ACCEPTED) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "release branch requires TEST_ACCEPTED, current=" + plan.getStatus());
        }
        return plan;
    }
}
