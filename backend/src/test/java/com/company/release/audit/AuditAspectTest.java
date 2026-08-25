package com.company.release.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Phase 18：审计切面落 operation_log（before/after 脱敏）。 */
class AuditAspectTest {

    public static class SampleService {
        @AuditLog(module = "release", action = "confirm")
        public Long confirm(Long planId, String token) {
            return planId;
        }
    }

    private OperationLogRepository repository;
    private SampleService proxy;

    @BeforeEach
    void setUp() {
        repository = mock(OperationLogRepository.class);
        AspectJProxyFactory factory = new AspectJProxyFactory(new SampleService());
        factory.addAspect(new AuditAspect(repository));
        proxy = factory.getProxy();
    }

    @Test
    void writesOperationLogWithMaskedArgs() {
        Long result = proxy.confirm(42L, "secret-token-value");

        assertThat(result).isEqualTo(42L);
        var captor = ArgumentCaptor.forClass(OperationLogEntity.class);
        verify(repository).save(captor.capture());
        var log = captor.getValue();
        assertThat(log.getModule()).isEqualTo("release");
        assertThat(log.getAction()).isEqualTo("confirm");
        assertThat(log.getTargetId()).isEqualTo("42");
        // token 必须脱敏
        assertThat(log.getAfterData()).contains("***").doesNotContain("secret-token-value");
    }

    @Test
    void auditFailureDoesNotBreakBusinessFlow() {
        var failingRepo = mock(OperationLogRepository.class);
        org.mockito.Mockito.when(failingRepo.save(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new RuntimeException("db down"));
        AspectJProxyFactory factory = new AspectJProxyFactory(new SampleService());
        factory.addAspect(new AuditAspect(failingRepo));
        SampleService p = factory.getProxy();

        // 审计失败不影响业务返回
        assertThat(p.confirm(7L, "t")).isEqualTo(7L);
    }
}
