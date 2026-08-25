package com.company.release.audit;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 敏感字段脱敏（规范 §二十五/§二十六）：password/token/secret/credential 一律打码。
 */
public final class SensitiveMasker {

    private static final String MASK = "***";
    private static final Pattern SENSITIVE_KEY = Pattern.compile(
            "(?i)(password|passwd|token|secret|credential|authorization|api[-_]?key)");

    private SensitiveMasker() {
    }

    /** 对扁平 KV 脱敏。 */
    public static Map<String, Object> mask(Map<String, Object> data) {
        if (data == null) {
            return Map.of();
        }
        var builder = new java.util.LinkedHashMap<String, Object>();
        data.forEach((k, v) -> builder.put(k, SENSITIVE_KEY.matcher(k).find() ? MASK : v));
        return builder;
    }

    /** 对 JSON 字符串按 "key":"value" 模式脱敏（宽松正则，避免引入额外依赖）。 */
    public static String maskJson(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }
        return json.replaceAll(
                "(\"(?i)" + keyPattern() + "\")\\s*:\\s*\"[^\"]*\"",
                "$1:\"" + MASK + "\"");
    }

    private static String keyPattern() {
        return "(?:[^\"]*(?:password|passwd|token|secret|credential|authorization|api[_-]?key)[^\"]*)";
    }
}
