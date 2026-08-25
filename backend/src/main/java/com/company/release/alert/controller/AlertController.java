package com.company.release.alert.controller;

import com.company.release.alert.application.AlertIngestService;
import com.company.release.alert.domain.AlertEntity;
import com.company.release.common.response.ApiResponse;
import com.company.release.iam.CurrentUser;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

/**
 * 报警 API（规范 §45/§59）：外部系统 Webhook 接入（projectKey+secret 鉴权后续接入）、列表、ACK、恢复。
 */
@RestController
public class AlertController {

    public record WebhookPayload(@NotBlank String title, String content, String level,
                                 String environment, String service, String labels) {
    }

    private final AlertIngestService ingestService;

    public AlertController(AlertIngestService ingestService) {
        this.ingestService = ingestService;
    }

    /** 外部报警接入入口；重复事件由指纹去重合并（ADR-007）。 */
    @PostMapping("/api/v1/alerts/webhook/{projectKey}")
    public ApiResponse<AlertEntity> webhook(@PathVariable String projectKey,
                                            @RequestBody WebhookPayload payload) {
        var alert = ingestService.ingest(new AlertIngestService.InboundAlert(
                projectKey, payload.title(), payload.content(), payload.level(),
                payload.environment(), payload.service(), payload.labels()));
        return ApiResponse.ok(alert);
    }

    @org.springframework.web.bind.annotation.GetMapping("/api/alerts")
    public ApiResponse<?> list(@RequestParam(required = false) Long projectId) {
        return ApiResponse.ok(ingestService.listByProject(projectId));
    }

    @PostMapping("/api/alerts/{id}/ack")
    public ApiResponse<Void> ack(@PathVariable Long id) {
        ingestService.acknowledge(CurrentUser.id(), id);
        return ApiResponse.ok();
    }

    @PostMapping("/api/alerts/{id}/resolve")
    public ApiResponse<Void> resolve(@PathVariable Long id) {
        ingestService.resolve(CurrentUser.id(), id);
        return ApiResponse.ok();
    }
}
