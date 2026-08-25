package com.company.release.audit;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 审计切面（Phase 18）：拦截 @AuditLog 方法，记录 before/after（脱敏后）到 operation_log。
 * 落库失败不得影响主流程（catch + ERROR 日志）。
 */
@Aspect
@Component
public class AuditAspect {

    private final OperationLogRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditAspect(OperationLogRepository repository) {
        this.repository = repository;
    }

    @Around("@annotation(auditLog)")
    public Object audit(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        Object result = pjp.proceed();
        try {
            var log = new OperationLogEntity();
            log.setModule(auditLog.module());
            log.setAction(auditLog.action());
            log.setTargetType(pjp.getSignature().getDeclaringType().getSimpleName());
            log.setRequestId(MDC.get("requestId"));
            log.setAfterData(SensitiveMasker.maskJson(objectMapper.writeValueAsString(argsOf(pjp))));
            if (result instanceof Long l) {
                log.setTargetId(String.valueOf(l));
            }
            repository.save(log);
        } catch (Exception e) {
            // 审计失败不阻断业务，但必须留痕错误
            org.slf4j.LoggerFactory.getLogger(AuditAspect.class)
                    .error("audit log failed: {}", e.getMessage());
        }
        return result;
    }

    private Map<String, Object> argsOf(ProceedingJoinPoint pjp) {
        var map = new LinkedHashMap<String, Object>();
        Object[] args = pjp.getArgs();
        String[] names = null;
        if (pjp.getSignature() instanceof org.aspectj.lang.reflect.MethodSignature ms) {
            names = ms.getParameterNames();
        }
        for (int i = 0; i < args.length; i++) {
            String name = (names != null && i < names.length && names[i] != null) ? names[i] : "arg" + i;
            map.put(name, String.valueOf(args[i]));
        }
        return SensitiveMasker.mask(map);
    }
}
