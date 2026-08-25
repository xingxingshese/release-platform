package com.company.release.audit;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 规范 §二十五：日志脱敏。 */
class SensitiveMaskerTest {

    @Test
    void maskSensitiveKeysInMap() {
        var masked = SensitiveMasker.mask(Map.of(
                "username", "alice",
                "password", "p@ss",
                "jenkinsToken", "abc"));
        assertThat(masked.get("username")).isEqualTo("alice");
        assertThat(masked.get("password")).isEqualTo("***");
        assertThat(masked.get("jenkinsToken")).isEqualTo("***");
    }

    @Test
    void maskSensitiveValuesInJson() {
        String json = "{\"url\":\"http://jenkins\",\"token\":\"abc123\",\"name\":\"job\"}";
        String out = SensitiveMasker.maskJson(json);
        assertThat(out).contains("\"token\":\"***\"");
        assertThat(out).contains("job");
        assertThat(out).doesNotContain("abc123");
    }

    @Test
    void blankJsonUnchanged() {
        assertThat(SensitiveMasker.maskJson(null)).isNull();
        assertThat(SensitiveMasker.maskJson("")).isEmpty();
    }
}
