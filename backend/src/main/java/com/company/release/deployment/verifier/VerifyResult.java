package com.company.release.deployment.verifier;

/**
 * 部署验证结果（规范 §31）。
 */
public enum VerifyResult {
    SUCCESS,
    FAILED,
    TIMEOUT,
    RUNNING,
    /** 版本不一致（规范 §33：运行版本 ≠ 本次发布版本）。 */
    VERSION_CHECK_FAILED
}
