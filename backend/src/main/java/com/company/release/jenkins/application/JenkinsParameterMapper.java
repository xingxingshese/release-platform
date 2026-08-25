package com.company.release.jenkins.application;

import com.company.release.common.exception.BusinessException;
import com.company.release.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Jenkins 参数映射（规范 §22）：按配置把平台字段映射为 Jenkins 参数。
 */
@Component
public class JenkinsParameterMapper {

    public record Mapping(String platformField, String jenkinsParameter, boolean required, String defaultValue) {
    }

    public Map<String, String> map(List<Mapping> mappings, Map<String, String> context) {
        Map<String, String> params = new HashMap<>();
        for (Mapping m : mappings) {
            String value = context.get(m.platformField());
            if (value == null || value.isBlank()) {
                value = m.defaultValue();
            }
            if (value == null && m.required()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "missing required parameter: " + m.jenkinsParameter());
            }
            if (value != null) {
                params.put(m.jenkinsParameter(), value);
            }
        }
        return params;
    }
}
