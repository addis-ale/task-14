package com.exam.system.service.impl;

import com.exam.system.config.SecurityProperties;
import com.exam.system.dto.ScopeDto;
import com.exam.system.entity.SysSession;
import com.exam.system.entity.SysUser;
import com.exam.system.entity.UserScope;
import com.exam.system.entity.UserStatus;
import com.exam.system.repository.SysSessionRepository;
import com.exam.system.repository.SysUserRepository;
import com.exam.system.repository.UserScopeRepository;
import com.exam.system.security.rbac.UserContext;
import com.exam.system.service.SessionService;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionServiceImpl implements SessionService {

    private final SysSessionRepository sessionRepository;
    private final SysUserRepository userRepository;
    private final UserScopeRepository userScopeRepository;
    private final ScopeAssembler scopeAssembler;
    private final SecurityProperties securityProperties;

    public SessionServiceImpl(SysSessionRepository sessionRepository,
                              SysUserRepository userRepository,
                              UserScopeRepository userScopeRepository,
                              ScopeAssembler scopeAssembler,
                              SecurityProperties securityProperties) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.userScopeRepository = userScopeRepository;
        this.scopeAssembler = scopeAssembler;
        this.securityProperties = securityProperties;
    }

    @Override
    @Transactional
    public Optional<UserContext> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        Optional<SysSession> sessionOptional = sessionRepository.findByToken(token);
        if (sessionOptional.isEmpty()) {
            return Optional.empty();
        }

        SysSession session = sessionOptional.get();
        LocalDateTime now = LocalDateTime.now();

        if (session.getExpiresAt().isBefore(now)) {
            sessionRepository.delete(session);
            return Optional.empty();
        }
        if (session.getCreatedAt().plusDays(securityProperties.getRememberDeviceMaxDays()).isBefore(now)) {
            sessionRepository.delete(session);
            return Optional.empty();
        }

        SysUser user = userRepository.findById(session.getUserId()).orElse(null);
        if (user == null || user.getStatus() == UserStatus.DISABLED) {
            sessionRepository.delete(session);
            return Optional.empty();
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            return Optional.empty();
        }

        session.setExpiresAt(now.plusMinutes(securityProperties.getSessionIdleTimeoutMinutes()));
        sessionRepository.save(session);

        UserContext context = new UserContext();
        context.setUserId(user.getId());
        context.setUsername(user.getUsername());
        context.setRoles(user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toList()));
        context.setActiveRole(session.getActiveRole());
        context.setToken(token);

        java.util.List<UserScope> scopes = userScopeRepository.findByUserId(user.getId());
        ScopeDto scopeDto = scopeAssembler.toDto(scopes);
        context.setScope(scopeDto);
        return Optional.of(context);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> resolveUserId(String token) {
        return sessionRepository.findByToken(token).map(SysSession::getUserId);
    }
}
