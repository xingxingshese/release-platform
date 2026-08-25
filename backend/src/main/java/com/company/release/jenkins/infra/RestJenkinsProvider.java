package com.company.release.jenkins.infra;

import com.company.release.common.exception.BusinessException;
import com.company.release.common.exception.ErrorCode;
import com.company.release.jenkins.api.JenkinsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Jenkins REST 实现（ADR-004）：地址与 API Token 全部配置化（jenkins.*），
 * 未配置时 fail-fast 抛 EXTERNAL_SERVICE_ERROR，绝不静默伪造构建状态。
 */
@Component
public class RestJenkinsProvider implements JenkinsProvider {

    private final String baseUrl;
    private final String token;
    private final String user;

    public RestJenkinsProvider(@Value("${jenkins.base-url:}") String baseUrl,
                               @Value("${jenkins.api-token:}") String token,
                               @Value("${jenkins.user:}") String user) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.user = user;
    }

    private RestClient client() {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "Jenkins not configured: set jenkins.base-url / jenkins.api-token");
        }
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(h -> h.setBasicAuth(user, token))
                .build();
    }

    @Override
    public long buildWithParameters(String jobName, Map<String, String> parameters) {
        var form = new org.springframework.util.LinkedMultiValueMap<String, String>();
        parameters.forEach(form::add);
        try {
            var resp = client().post()
                    .uri("/job/{job}/buildWithParameters", jobName)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
            String location = resp.getHeaders().getFirst("Location");
            if (location == null) {
                throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                        "Jenkins did not return queue Location header");
            }
            return Long.parseLong(location.replaceAll(".*/queue/item/(\\d+).*", "$1"));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "jenkins trigger failed: " + e.getMessage());
        }
    }

    @Override
    public Long getBuildNumberFromQueue(long queueId) {
        QueueResponse q = client().get()
                .uri("/queue/item/{id}/api/json", queueId)
                .retrieve()
                .body(QueueResponse.class);
        if (q == null || q.executable() == null) {
            return null; // 仍在排队
        }
        return q.executable().number();
    }

    @Override
    public BuildInfo getBuild(String jobName, long buildNumber) {
        BuildResponse b = client().get()
                .uri("/job/{job}/{n}/api/json", jobName, buildNumber)
                .retrieve()
                .body(BuildResponse.class);
        if (b == null) {
            return new BuildInfo(BuildStatus.UNKNOWN, buildNumber, "");
        }
        BuildStatus status;
        if (b.building()) {
            status = BuildStatus.RUNNING;
        } else if ("SUCCESS".equals(b.result())) {
            status = BuildStatus.SUCCESS;
        } else if ("ABORTED".equals(b.result())) {
            status = BuildStatus.ABORTED;
        } else {
            status = BuildStatus.FAILURE;
        }
        return new BuildInfo(status, buildNumber, b.url() == null ? "" : b.url());
    }

    @Override
    public String getConsoleText(String jobName, long buildNumber) {
        return client().get()
                .uri("/job/{job}/{n}/consoleText", jobName, buildNumber)
                .retrieve()
                .body(String.class);
    }

    @Override
    public void stopBuild(String jobName, long buildNumber) {
        client().post()
                .uri("/job/{job}/{n}/stop", jobName, buildNumber)
                .retrieve()
                .toBodilessEntity();
    }

    record QueueResponse(Executable executable) {
        record Executable(long number) {
        }
    }

    record BuildResponse(boolean building, String result, String url) {
    }
}
