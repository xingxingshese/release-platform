package com.company.release.common.exception;

/** 资源不存在。 */
public class NotFoundException extends BusinessException {

    public NotFoundException(String resourceType, String id) {
        super(ErrorCode.NOT_FOUND, resourceType + " not found: " + id);
    }
}
