package com.company.release.iam;

import com.company.release.common.exception.BusinessException;
import com.company.release.common.exception.ErrorCode;
import com.company.release.iam.repository.PermissionQueryDao;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 权限判定（规范 §56/§57）。"*" 表示全部权限（SUPER_ADMIN）。
 */
@Service
public class PermissionChecker {

    public static final String PROD_EXECUTE = "release:prod:execute";
    public static final String PROD_CONFIRM = "release:prod:confirm";
    public static final String RELEASE_EDIT = "release:edit";
    public static final String TEST_ACCEPT = "release:test-accept";
    public static final String CONFIG_MANAGE = "config:manage";

    private final PermissionQueryDao permissionQueryDao;

    public PermissionChecker(PermissionQueryDao permissionQueryDao) {
        this.permissionQueryDao = permissionQueryDao;
    }

    public boolean hasPermission(Long userId, String permission) {
        List<String> codes = permissionQueryDao.findPermissionCodesByUserId(userId);
        return codes.contains("*") || codes.contains(permission);
    }

    /** 无权限抛 PERMISSION_DENIED。 */
    public void checkPermission(Long userId, String permission) {
        if (!hasPermission(userId, permission)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED,
                    "permission denied: " + permission);
        }
    }
}
