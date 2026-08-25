package com.company.release.release.controller;

import com.company.release.common.response.ApiResponse;
import com.company.release.iam.CurrentUser;
import com.company.release.release.application.*;
import com.company.release.release.domain.model.ReleasePlanEntity;
import com.company.release.release.repository.ReleasePlanRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 发布计划/发布任务 API（规范 §59）。
 */
@RestController
@RequestMapping("/api")
public class ReleaseController {

    private final ReleasePlanRepository planRepository;
    private final ReleasePlanService planService;
    private final ReleaseOrchestrator orchestrator;
    private final AcceptanceService acceptanceService;
    private final ReleaseBranchService branchService;
    private final ProdConfirmService prodConfirmService;

    public ReleaseController(ReleasePlanRepository planRepository,
                             ReleasePlanService planService,
                             ReleaseOrchestrator orchestrator,
                             AcceptanceService acceptanceService,
                             ReleaseBranchService branchService,
                             ProdConfirmService prodConfirmService) {
        this.planRepository = planRepository;
        this.planService = planService;
        this.orchestrator = orchestrator;
        this.acceptanceService = acceptanceService;
        this.branchService = branchService;
        this.prodConfirmService = prodConfirmService;
    }

    public record CreatePlanRequest(@NotBlank Long projectId, @NotBlank String name,
                                    String versionName, String description, String environments) {
    }

    // ---- 发布计划 CRUD ----

    @PostMapping("/release-plans")
    public ApiResponse<ReleasePlanEntity> create(@RequestBody @Valid CreatePlanRequest req) {
        var cmd = new ReleasePlanService.CreatePlanCmd(req.projectId(), req.name(),
                req.versionName(), req.description(), null, req.environments());
        return ApiResponse.ok(planService.create(CurrentUser.id(), cmd));
    }

    @GetMapping("/release-plans")
    public ApiResponse<List<ReleasePlanEntity>> list() {
        return ApiResponse.ok(planRepository.findAll());
    }

    @GetMapping("/release-plans/{id}")
    public ApiResponse<ReleasePlanEntity> get(@PathVariable Long id) {
        return ApiResponse.ok(planRepository.findById(id)
                .orElseThrow(() -> new com.company.release.common.exception.BusinessException(
                        com.company.release.common.exception.ErrorCode.NOT_FOUND, "not found")));
    }

    // ---- 状态流转动作 ----

    @PostMapping("/release-plans/{id}/ready")
    public ApiResponse<Void> ready(@PathVariable Long id) {
        planService.ready(CurrentUser.id(), id);
        return ApiResponse.ok();
    }

    /** 测试环境发布：merge → Jenkins（冲突暂停）。 */
    @PostMapping("/release-plans/{id}/test-release")
    public ApiResponse<?> testRelease(@PathVariable Long id) {
        return ApiResponse.ok(orchestrator.startTestRelease(id));
    }

    /** 测试验收。 */
    @PostMapping("/release-plans/{id}/test-accept")
    public ApiResponse<Void> accept(@PathVariable Long id) {
        acceptanceService.accept(CurrentUser.id(), id);
        return ApiResponse.ok();
    }

    @PostMapping("/release-plans/{id}/test-reject")
    public ApiResponse<Void> reject(@PathVariable Long id) {
        acceptanceService.reject(CurrentUser.id(), id);
        return ApiResponse.ok();
    }

    /** 创建 Release Branch（验收通过后）。 */
    @PostMapping("/release-plans/{id}/create-release-branch")
    public ApiResponse<Void> createBranch(@PathVariable Long id) {
        branchService.create(CurrentUser.id(), id);
        return ApiResponse.ok();
    }

    /** 生产确认：WAIT_PROD_CONFIRM → COMPLETED。 */
    @PostMapping("/release-plans/{id}/prod-confirm")
    public ApiResponse<Void> confirm(@PathVariable Long id) {
        prodConfirmService.confirm(CurrentUser.id(), id);
        return ApiResponse.ok();
    }
}
