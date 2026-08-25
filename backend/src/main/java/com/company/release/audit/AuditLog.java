package com.company.release.audit;

import java.lang.annotation.*;

/** 生产/管理操作审计注解（规范 §二-7、§五十八）：标注在 Service 方法上，切面自动落 operation_log。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /** 模块名，如 release / alert / config。 */
    String module();

    /** 动作名，如 confirm / deploy-prod。 */
    String action();
}
