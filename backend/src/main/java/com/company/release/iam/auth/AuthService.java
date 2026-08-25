package com.company.release.iam.auth;

import com.company.release.iam.repository.PermissionQueryDao;
import com.company.release.iam.domain.UserEntity;
import com.company.release.iam.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 登录认证：校验凭证 → 签发携带权限的 JWT。
 * 失败统一返回"invalid credentials"，不泄露用户是否存在。
 */
@Service
public class AuthService {

    public record LoginResult(String token, Long userId, String username, List<String> permissions) {
    }

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PermissionQueryDao permissionQueryDao;

    public AuthService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       PermissionQueryDao permissionQueryDao) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.permissionQueryDao = permissionQueryDao;
    }

    public LoginResult login(String username, String rawPassword) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.company.release.common.exception.BusinessException(
                        com.company.release.common.exception.ErrorCode.AUTH_ERROR, "invalid credentials"));
        if (!user.isEnabled()) {
            throw new com.company.release.common.exception.BusinessException(
                    com.company.release.common.exception.ErrorCode.AUTH_ERROR, "user disabled: " + username);
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new com.company.release.common.exception.BusinessException(
                    com.company.release.common.exception.ErrorCode.AUTH_ERROR, "invalid credentials");
        }
        List<String> permissions = permissionQueryDao.findPermissionCodesByUserId(user.getId());
        String token = jwtService.issue(user.getId(), user.getUsername(), permissions);
        return new LoginResult(token, user.getId(), user.getUsername(), permissions);
    }
}
