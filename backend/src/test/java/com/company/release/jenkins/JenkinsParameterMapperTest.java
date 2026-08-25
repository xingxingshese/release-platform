package com.company.release.jenkins;

import com.company.release.common.exception.BusinessException;
import com.company.release.jenkins.application.JenkinsParameterMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 规范 §22：不同 Job 参数不同 → 参数映射配置化，平台动态组装。
 */
class JenkinsParameterMapperTest {

    private final JenkinsParameterMapper mapper = new JenkinsParameterMapper();

    @Test
    void mapsPlatformFieldsToJenkinsParameters() {
        var mappings = List.of(
                new JenkinsParameterMapper.Mapping("sourceBranch", "BRANCH", true, null),
                new JenkinsParameterMapper.Mapping("environment", "ENV", true, null),
                new JenkinsParameterMapper.Mapping("version", "IMAGE_TAG", false, "latest"));
        var context = Map.of("sourceBranch", "feature/order-123", "environment", "TEST");

        Map<String, String> params = mapper.map(mappings, context);

        assertThat(params)
                .containsEntry("BRANCH", "feature/order-123")
                .containsEntry("ENV", "TEST")
                .containsEntry("IMAGE_TAG", "latest"); // default 生效
    }

    @Test
    void missingRequiredParameterRejected() {
        var mappings = List.of(
                new JenkinsParameterMapper.Mapping("releasePlanId", "RELEASE_PLAN_ID", true, null));
        assertThatThrownBy(() -> mapper.map(mappings, Map.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("RELEASE_PLAN_ID");
    }

    @Test
    void unknownContextFieldsAreIgnored() {
        var mappings = List.of(new JenkinsParameterMapper.Mapping("a", "A", false, null));
        var params = mapper.map(mappings, Map.of("a", "1", "zzz-unused", "2"));
        assertThat(params).containsOnlyKeys("A");
    }
}
