package com.company.release.common.exception;

/** 并发冲突 / 重复操作（如发布任务已在运行）。 */
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
