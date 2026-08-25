package com.company.release.iam.auth;

import com.company.release.iam.domain.UserEntity;
import com.company.release.iam.repository.PermissionQueryDao;
import com.company.release.iam.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository userRepository;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        jwtService = mock(JwtService.class);
        var permissionDao = mock(PermissionQueryDao.class);
        authService = new AuthService(userRepository, new BCryptPasswordEncoder(),
                jwtService, permissionDao);
    }

    @Test
    void loginWithWrongPasswordFailsAuthError() {
        var user = user("alice", "$2a$10$wronghashwronghashwronghashwronghashwronghashwronghash");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> authService.login("alice", "secret"))
                .hasMessageContaining("invalid credentials");
    }

    @Test
    void loginDisabledUserFails() {
        var user = user("bob", null);
        user.setEnabled(false);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> authService.login("bob", "x"))
                .hasMessageContaining("disabled");
    }

    @Test
    void loginUnknownUserFails() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login("ghost", "x"))
                .hasMessageContaining("invalid credentials");
    }

    @Test
    void loginSuccessReturnsTokenAndPermissions() {
        var encoder = new BCryptPasswordEncoder();
        var user = user("carol", encoder.encode("pass123"));
        when(userRepository.findByUsername("carol")).thenReturn(Optional.of(user));
        when(jwtService.issue(user.getId(), "carol", List.of())).thenReturn("token-abc");

        var result = authService.login("carol", "pass123");
        org.assertj.core.api.Assertions.assertThat(result.token()).isEqualTo("token-abc");
        org.assertj.core.api.Assertions.assertThat(result.username()).isEqualTo("carol");
    }

    private UserEntity user(String name, String hash) {
        var u = new UserEntity();
        u.setId(7L);
        u.setUsername(name);
        u.setPasswordHash(hash);
        u.setEnabled(true);
        return u;
    }
}
