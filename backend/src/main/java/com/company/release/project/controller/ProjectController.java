package com.company.release.project.controller;

import com.company.release.common.response.ApiResponse;
import com.company.release.iam.CurrentUser;
import com.company.release.project.application.ProjectService;
import com.company.release.project.domain.ProjectEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目 API（规范 §60 /api/admin/projects）。
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    public record CreateProjectRequest(@jakarta.validation.constraints.NotBlank String code,
                                       @jakarta.validation.constraints.NotBlank String name,
                                       String description,
                                       @jakarta.validation.constraints.NotBlank String projectType) {
    }

    public record AddMemberRequest(@jakarta.validation.constraints.NotNull Long userId,
                                   @jakarta.validation.constraints.NotBlank String role) {
    }

    public record AddServiceRequest(@jakarta.validation.constraints.NotBlank String code,
                                    @jakarta.validation.constraints.NotBlank String name,
                                    @jakarta.validation.constraints.NotBlank String type) {
    }

    @PostMapping
    public ApiResponse<ProjectEntity> create(@RequestBody @Valid CreateProjectRequest req) {
        var cmd = new ProjectService.CreateProjectCmd(req.code(), req.name(), req.description(), req.projectType());
        return ApiResponse.ok(projectService.create(CurrentUser.id(), cmd));
    }

    @GetMapping
    public ApiResponse<List<ProjectEntity>> list() {
        return ApiResponse.ok(projectService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectEntity> get(@PathVariable Long id) {
        return ApiResponse.ok(projectService.getById(id));
    }

    @PostMapping("/{id}/members")
    public ApiResponse<Void> addMember(@PathVariable Long id, @RequestBody @Valid AddMemberRequest req) {
        projectService.addMember(id, req.userId(), req.role());
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/members")
    public ApiResponse<?> members(@PathVariable Long id) {
        return ApiResponse.ok(projectService.members(id));
    }

    @PostMapping("/{id}/services")
    public ApiResponse<Void> addService(@PathVariable Long id, @RequestBody @Valid AddServiceRequest req) {
        projectService.addService(id, CurrentUser.id(), new ProjectService.AddServiceCmd(req.code(), req.name(), req.type()));
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/services")
    public ApiResponse<?> services(@PathVariable Long id) {
        return ApiResponse.ok(projectService.services(id));
    }
}
