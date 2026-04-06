package com.exam.system.service;

import com.exam.system.config.SecurityProperties;
import com.exam.system.dto.auth.LoginRequest;
import com.exam.system.entity.SysRole;
import com.exam.system.entity.SysUser;
import com.exam.system.entity.UserSecurityPolicy;
import com.exam.system.entity.UserStatus;
import com.exam.system.exception.BusinessException;
import com.exam.system.repository.AuditLogRepository;
import com.exam.system.repository.LoginAttemptRepository;
import com.exam.system.repository.PasswordHistoryRepository;
import com.exam.system.repository.SysSessionRepository;
import com.exam.system.repository.SysUserRepository;
import com.exam.system.repository.UserScopeRepository;
import com.exam.system.repository.UserSecurityPolicyRepository;
import com.exam.system.security.crypto.PasswordCryptoService;
import com.exam.system.service.impl.AuthServiceImpl;
import com.exam.system.service.impl.PasswordPolicyValidator;
import com.exam.system.service.impl.ScopeAssembler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private SysUserRepository userRepository;
    @Mock private SysSessionRepository sessionRepository;
    @Mock private LoginAttemptRepository loginAttemptRepository;
    @Mock private PasswordHistoryRepository passwordHistoryRepository;
    @Mock private UserSecurityPolicyRepository securityPolicyRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private PasswordCryptoService passwordCryptoService;
    @Mock private ScopeAssembler scopeAssembler;
    @Mock private UserScopeRepository userScopeRepository;
    @Mock private SecurityProperties securityProperties;
    @Mock private PasswordPolicyValidator passwordPolicyValidator;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository, sessionRepository, loginAttemptRepository,
                passwordHistoryRepository, securityPolicyRepository,
                auditLogRepository, passwordCryptoService, scopeAssembler,
                userScopeRepository, securityProperties, passwordPolicyValidator);
    }

    @Test
    @DisplayName("Login fails with invalid username")
    void loginFailsWithInvalidUsername() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        LoginRequest req = loginRequest("nonexistent", "Password123!!");
        assertThatThrownBy(() -> authService.login(req, "127.0.0.1", "test-fp"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Login fails with wrong password")
    void loginFailsWithWrongPassword() {
        SysUser user = createUser(1L, "admin", "hashedpw");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordCryptoService.matches("WrongPass123!!", "hashedpw")).thenReturn(false);

        LoginRequest req = loginRequest("admin", "WrongPass123!!");
        assertThatThrownBy(() -> authService.login(req, "127.0.0.1", "test-fp"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Login fails when account is locked")
    void loginFailsWhenAccountLocked() {
        SysUser user = createUser(1L, "admin", "hashedpw");
        user.setLockedUntil(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        LoginRequest req = loginRequest("admin", "Password123!!");
        assertThatThrownBy(() -> authService.login(req, "127.0.0.1", "test-fp"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Login succeeds with valid credentials")
    void loginSucceedsWithValidCredentials() {
        SysUser user = createUser(1L, "admin", "hashedpw");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordCryptoService.matches("ValidPass123!!", "hashedpw")).thenReturn(true);
        when(securityPolicyRepository.findById(1L)).thenReturn(Optional.empty());
        when(securityProperties.getSessionIdleTimeoutMinutes()).thenReturn(30);
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoginRequest req = loginRequest("admin", "ValidPass123!!");
        var response = authService.login(req, "127.0.0.1", "test-fp");

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotBlank();
    }

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        return req;
    }

    private SysUser createUser(Long id, String username, String password) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash(password);
        user.setStatus(UserStatus.ACTIVE);
        SysRole role = new SysRole();
        role.setName("ADMIN");
        user.setRoles(Set.of(role));
        return user;
    }
}
