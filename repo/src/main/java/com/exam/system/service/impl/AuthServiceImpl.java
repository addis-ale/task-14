package com.exam.system.service.impl;

import com.exam.system.config.InputSanitizer;
import com.exam.system.config.SecurityProperties;
import com.exam.system.dto.auth.ChangePasswordRequest;
import com.exam.system.dto.auth.LoginRequest;
import com.exam.system.dto.auth.LoginResponse;
import com.exam.system.dto.auth.SwitchRoleResponse;
import com.exam.system.dto.auth.UserProfileDto;
import com.exam.system.entity.AuditLog;
import com.exam.system.entity.LoginAttempt;
import com.exam.system.entity.PasswordHistory;
import com.exam.system.entity.SysRole;
import com.exam.system.entity.SysSession;
import com.exam.system.entity.SysUser;
import com.exam.system.entity.UserStatus;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ErrorCode;
import com.exam.system.repository.AuditLogRepository;
import com.exam.system.repository.LoginAttemptRepository;
import com.exam.system.repository.PasswordHistoryRepository;
import com.exam.system.repository.SysSessionRepository;
import com.exam.system.repository.SysUserRepository;
import com.exam.system.repository.UserSecurityPolicyRepository;
import com.exam.system.service.AuthService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core authentication, lockout, session management and password policy logic.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SysUserRepository userRepository;
    private final SysSessionRepository sessionRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final UserSecurityPolicyRepository userSecurityPolicyRepository;
    private final AuditLogRepository auditLogRepository;
    private final com.exam.system.security.crypto.PasswordCryptoService passwordCryptoService;
    private final ScopeAssembler scopeAssembler;
    private final com.exam.system.repository.UserScopeRepository userScopeRepository;
    private final SecurityProperties securityProperties;
    private final PasswordPolicyValidator passwordPolicyValidator;

    public AuthServiceImpl(SysUserRepository userRepository,
                           SysSessionRepository sessionRepository,
                           LoginAttemptRepository loginAttemptRepository,
                           PasswordHistoryRepository passwordHistoryRepository,
                           UserSecurityPolicyRepository userSecurityPolicyRepository,
                           AuditLogRepository auditLogRepository,
                           com.exam.system.security.crypto.PasswordCryptoService passwordCryptoService,
                           ScopeAssembler scopeAssembler,
                           com.exam.system.repository.UserScopeRepository userScopeRepository,
                           SecurityProperties securityProperties,
                           PasswordPolicyValidator passwordPolicyValidator) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.userSecurityPolicyRepository = userSecurityPolicyRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordCryptoService = passwordCryptoService;
        this.scopeAssembler = scopeAssembler;
        this.userScopeRepository = userScopeRepository;
        this.securityProperties = securityProperties;
        this.passwordPolicyValidator = passwordPolicyValidator;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String fallbackFingerprint) {
        String username = InputSanitizer.sanitize(request.getUsername());
        String password = request.getPassword();
        String fingerprint = request.getDeviceFingerprint() == null || request.getDeviceFingerprint().isBlank()
                ? fallbackFingerprint
                : request.getDeviceFingerprint();

        SysUser user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            recordAttempt(null, ipAddress, fingerprint, false);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (isLocked(user)) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, HttpStatus.LOCKED,
                    "Account locked until " + user.getLockedUntil());
        }

        if (!passwordCryptoService.matches(password, user.getPasswordHash())) {
            recordAttempt(user.getId(), ipAddress, fingerprint, false);
            applyLockoutIfNeeded(user, ipAddress);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        recordAttempt(user.getId(), ipAddress, fingerprint, true);
        user.setStatus(UserStatus.ACTIVE);
        user.setLockedUntil(null);
        userRepository.save(user);

        boolean concurrentAllowed = userSecurityPolicyRepository.findById(user.getId())
                .map(p -> p.isConcurrentSessionsAllowed())
                .orElse(false);
        if (!concurrentAllowed) {
            sessionRepository.findByUserId(user.getId()).forEach(sessionRepository::delete);
        }

        Set<String> roleNames = user.getRoles().stream().map(SysRole::getName).collect(Collectors.toSet());
        String activeRole = resolveDefaultRole(roleNames);

        SysSession session = new SysSession();
        session.setUserId(user.getId());
        session.setToken(UUID.randomUUID().toString());
        session.setActiveRole(activeRole);
        session.setDeviceFingerprint(fingerprint);
        session.setExpiresAt(LocalDateTime.now().plusMinutes(securityProperties.getSessionIdleTimeoutMinutes()));
        session.setCreatedAt(LocalDateTime.now());
        sessionRepository.save(session);

        LoginResponse response = new LoginResponse();
        response.setToken(session.getToken());
        response.setExpiresIn(securityProperties.getSessionIdleTimeoutMinutes() * 60L);
        response.setUser(toUserProfile(user, activeRole));
        return response;
    }

    @Override
    @Transactional
    public void logout(String token) {
        sessionRepository.deleteByToken(token);
    }

    @Override
    @Transactional
    public SwitchRoleResponse switchActiveRole(Long userId, String token, String role) {
        SysSession session = sessionRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_INVALID, HttpStatus.UNAUTHORIZED,
                        "Session not found"));
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Cannot switch role for this session");
        }

        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "User not found"));

        boolean hasRole = user.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase(role));
        if (!hasRole) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Role not assigned to current user");
        }

        session.setActiveRole(role);
        sessionRepository.save(session);

        SwitchRoleResponse response = new SwitchRoleResponse();
        response.setActiveRole(role);
        response.setScopes(scopeAssembler.toDto(userScopeRepository.findByUserId(userId)));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto me(Long userId, String token) {
        SysSession session = sessionRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_INVALID, HttpStatus.UNAUTHORIZED,
                        "Session not found"));
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "User not found"));
        return toUserProfile(user, session.getActiveRole());
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordCryptoService.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED,
                    "Current password is incorrect");
        }

        passwordPolicyValidator.validate(request.getNewPassword());

        List<PasswordHistory> latestFive = passwordHistoryRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId);
        boolean reused = latestFive.stream().anyMatch(ph -> passwordCryptoService.matches(request.getNewPassword(), ph.getPasswordHash()))
                || passwordCryptoService.matches(request.getNewPassword(), user.getPasswordHash());
        if (reused) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                    "New password must not match the last 5 passwords");
        }

        user.setSalt(randomSalt());
        user.setPasswordHash(passwordCryptoService.hash(request.getNewPassword()));
        userRepository.save(user);

        PasswordHistory history = new PasswordHistory();
        history.setUserId(userId);
        history.setPasswordHash(user.getPasswordHash());
        passwordHistoryRepository.save(history);
    }

    private UserProfileDto toUserProfile(SysUser user, String activeRole) {
        UserProfileDto profile = new UserProfileDto();
        profile.setId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setRoles(user.getRoles().stream().map(SysRole::getName).sorted().collect(Collectors.toList()));
        profile.setActiveRole(activeRole);
        profile.setScopes(scopeAssembler.toDto(userScopeRepository.findByUserId(user.getId())));
        return profile;
    }

    private void recordAttempt(Long userId, String ipAddress, String fingerprint, boolean success) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setUserId(userId);
        attempt.setIpAddress(ipAddress);
        attempt.setDeviceFingerprint(fingerprint);
        attempt.setSuccess(success);
        loginAttemptRepository.save(attempt);
    }

    private void applyLockoutIfNeeded(SysUser user, String ipAddress) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        List<LoginAttempt> attempts = loginAttemptRepository
                .findTop20ByUserIdAndAttemptedAtAfterOrderByAttemptedAtDesc(user.getId(), cutoff)
                .stream()
                .sorted(Comparator.comparing(LoginAttempt::getAttemptedAt).reversed())
                .toList();

        int consecutiveFailures = 0;
        for (LoginAttempt attempt : attempts) {
            if (attempt.isSuccess()) {
                break;
            }
            consecutiveFailures++;
        }

        if (consecutiveFailures >= 5) {
            user.setStatus(UserStatus.LOCKED);
            user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
            userRepository.save(user);

            AuditLog lockLog = new AuditLog();
            lockLog.setUserId(user.getId());
            lockLog.setAction("ACCOUNT_LOCKED");
            lockLog.setResource("auth");
            lockLog.setResourceId(user.getId().toString());
            lockLog.setIpAddress(ipAddress);
            lockLog.setDetailsJson("{\"reason\":\"5 consecutive failed logins in 30 minutes\"}");
            auditLogRepository.save(lockLog);
        }
    }

    private String resolveDefaultRole(Set<String> roleNames) {
        if (roleNames.contains("ADMIN")) {
            return "ADMIN";
        }
        return roleNames.stream().findFirst().orElse("STUDENT");
    }

    private boolean isLocked(SysUser user) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now());
    }

    private String randomSalt() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
