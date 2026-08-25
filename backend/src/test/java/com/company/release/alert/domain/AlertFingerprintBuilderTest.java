package com.company.release.alert.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ADR-007 fingerprint 规则：project+service+environment+alertRule+labels。
 */
class AlertFingerprintBuilderTest {

    private final AlertFingerprintBuilder builder = new AlertFingerprintBuilder();

    @Test
    void sameInputsProduceSameFingerprint() {
        String a = builder.build("order", "order-service", "prod", "error-rate-high", "db=primary,dc=bj");
        String b = builder.build("order", "order-service", "prod", "error-rate-high", "dc=bj,db=primary");
        assertThat(a).isEqualTo(b); // labels 顺序无关
    }

    @Test
    void differentServiceProducesDifferentFingerprint() {
        String a = builder.build("order", "order-service", "prod", "error-rate-high", "");
        String b = builder.build("order", "stock-service", "prod", "error-rate-high", "");
        assertThat(a).isNotEqualTo(b);
    }
}
