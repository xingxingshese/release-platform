package com.company.release.requirement.controller;

import com.company.release.common.response.ApiResponse;
import com.company.release.iam.CurrentUser;
import com.company.release.requirement.application.RequirementService;
import com.company.release.requirement.domain.RequirementEntity;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 需求 API（规范 §6）。
 */
@RestController
@RequestMapping("/api/projects/{projectId}/requirements")
public class RequirementController {

    public record CreateManualRequest(@NotBlank String title, String description, String priority) {
    }

    private final RequirementService requirementService;

    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @PostMapping
    public ApiResponse<RequirementEntity> create(@PathVariable Long projectId,
                                                 @RequestBody CreateManualRequest req) {
        var r = requirementService.createManual(projectId, req.title(), req.description(), req.priority());
        r.setOwnerId(CurrentUser.id());
        return ApiResponse.ok(r);
    }

    @GetMapping
    public ApiResponse<List<RequirementEntity>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(requirementService.listByProject(projectId));
    }

    @PostMapping("/import/{sourceType}/{externalId}")
    public ApiResponse<RequirementEntity> importReq(@PathVariable Long projectId,
                                                    @PathVariable String sourceType,
                                                    @PathVariable String externalId) {
        return ApiResponse.ok(requirementService.importFromProvider(projectId, sourceType, externalId));
    }

    @GetMapping("/external/{sourceType}")
    public ApiResponse<?> searchExternal(@PathVariable String sourceType,
                                         @RequestParam(defaultValue = "") String keyword) {
        return ApiResponse.ok(requirementService.search(sourceType, keyword));
    }
}
