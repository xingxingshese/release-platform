package com.company.release.iam.auth;

import com.company.release.iam.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // HS256 要求密钥 >= 256bit
        jwtService = new JwtService("0123456789abcdef0123456789abcdef0123456789abcdef", Duration.ofHours(8));
    }

    @Test
    void issueAndParseRoundTrip() {
        String token = jwtService.issue(10086L, "zhangsan", List.of("release:prod:execute", "project:manage"));
        var claims = jwtService.parse(token);
        assertThat(claims.userId()).isEqualTo(10086L);
        assertThat(claims.username()).isEqualTo("zhangsan");
        assertThat(claims.permissions()).containsExactlyInAnyOrder("release:prod:execute", "project:manage");
    }

    @Test
    void expiredTokenRejected() {
        var shortLived = new JwtService("0123456789abcdef0123456789abcdef0123456789abcdef", Duration.ofMillis(-1));
        String token = shortLived.issue(1L, "a", List.of());
        assertThatThrownBy(() -> shortLived.parse(token))
                .isInstanceOf(JwtService.InvalidTokenException.class);
    }

    @Test
    void tamperedTokenRejected() {
        String token = jwtService.issue(1L, "a", List.of("x"));
        String tampered = token.substring(0, token.length() - 4) + "AAAA";
        assertThatThrownBy(() -> jwtService.parse(tampered))
                .isInstanceOf(JwtService.InvalidTokenException.class);
    }
}
