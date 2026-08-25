package com.company.release.admin;

import com.company.release.common.response.ApiResponse;
import com.company.release.iam.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 配置中心 API（spec 016）：版本保存/历史/diff。 */
@RestController
@RequestMapping("/api/admin/configs")
@Tag(name = "Admin Config", description = "管理员配置中心与配置版本")
public class AdminConfigController {

    private final AdminConfigService service;

    public AdminConfigController(AdminConfigService service) {
        this.service = service;
    }

    @PostMapping("/{type}/{key}/versions")
    @Operation(summary = "保存配置新版本（版本号单调递增）")
    public ApiResponse<Map<String, Object>> save(@PathVariable String type, @PathVariable String key,
                                                 @RequestBody SaveReq req) {
        var saved = service.saveVersion(type, key, req.content(), CurrentUser.id(), req.reason());
        return ApiResponse.ok(Map.of(
                "id", saved.getId(),
                "version", saved.getVersion(),
                "changedBy", saved.getChangedBy()));
    }

    @GetMapping("/{type}/{key}/versions")
    @Operation(summary = "版本历史（新→旧）")
    public ApiResponse<List<Map<String, Object>>> history(@PathVariable String type, @PathVariable String key) {
        var list = service.versions(type, key).stream()
                .map(v -> Map.<String, Object>of(
                        "version", v.getVersion(),
                        "changedBy", v.getChangedBy(),
                        "reason", v.getChangeReason() == null ? "" : v.getChangeReason()))
                .toList();
        return ApiResponse.ok(list);
    }

    @GetMapping("/{type}/{key}/diff")
    @Operation(summary = "字段级对比 当前值 vs 新值")
    public ApiResponse<List<AdminConfigService.DiffItem>> diff(@PathVariable String type,
                                                               @PathVariable String key,
                                                               @RequestParam int v1,
                                                               @RequestParam int v2) {
        return ApiResponse.ok(service.diff(type, key, v1, v2));
    }

    public record SaveReq(String content, String reason) {
    }
}
