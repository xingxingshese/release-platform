package com.company.release.iam;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户上下文。JwtAuthenticationFilter 将 UserPrincipal 放入 Authentication。
 */
public final class CurrentUser {

    public record UserPrincipal(Long userId, String username) {
    }

    private CurrentUser() {
    }

    public static Long id() {
        UserPrincipal p = principal();
        if (p == null) {
            throw new com.company.release.common.exception.BusinessException(
                    com.company.release.common.exception.ErrorCode.AUTH_ERROR, "not authenticated");
        }
        return p.userId();
    }

    public static String name() {
        UserPrincipal p = principal();
        return p == null ? null : p.username();
    }

    public static boolean isAuthenticated() {
        return principal() != null;
    }

    private static UserPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return up;
        }
        return null;
    }
}
