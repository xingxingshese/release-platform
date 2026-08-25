package com.company.release.alert.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.stream.Collectors;

/**
 * 报警指纹生成（ADR-007）：
 * fingerprint = sha256(project + service + environment + alertRule + sorted(labels))
 * labels 顺序无关，保证同一报警不同 label 排序产生相同指纹。
 */
@org.springframework.stereotype.Component
public class AlertFingerprintBuilder {

    public String build(String project, String service, String environment,
                        String alertRule, String labels) {
        String normalizedLabels = Arrays.stream((labels == null ? "" : labels).split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .sorted()
                .collect(Collectors.joining(","));
        String raw = String.join("|",
                nullSafe(project), nullSafe(service), nullSafe(environment),
                nullSafe(alertRule), normalizedLabels);
        return sha256Hex(raw);
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
