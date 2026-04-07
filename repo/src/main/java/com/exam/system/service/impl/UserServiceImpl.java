package com.exam.system.service.impl;

import com.exam.system.config.InputSanitizer;
import com.exam.system.dto.ScopeDto;
import com.exam.system.dto.user.ConcurrentSessionsRequest;
import com.exam.system.dto.user.CreateUserRequest;
import com.exam.system.dto.user.PaginationDto;
import com.exam.system.dto.user.UpdateUserRequest;
import com.exam.system.dto.user.UserListData;
import com.exam.system.dto.user.UserRequestScopeDto;
import com.exam.system.dto.user.UserResponse;
import com.exam.system.entity.AuditLog;
import com.exam.system.entity.PasswordHistory;
import com.exam.system.entity.SysRole;
import com.exam.system.entity.SysUser;
import com.exam.system.entity.UserScope;
import com.exam.system.entity.UserStatus;
import com.exam.system.entity.UserSecurityPolicy;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ErrorCode;
import com.exam.system.repository.AuditLogRepository;
import com.exam.system.repository.PasswordHistoryRepository;
import com.exam.system.repository.SysRoleRepository;
import com.exam.system.repository.SysUserRepository;
import com.exam.system.repository.UserScopeRepository;
import com.exam.system.repository.UserSecurityPolicyRepository;
import com.exam.system.service.UserService;
import com.exam.system.service.NotificationPreferenceService;
import jakarta.persistence.criteria.Join;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final UserScopeRepository userScopeRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final UserSecurityPolicyRepository userSecurityPolicyRepository;
    private final AuditLogRepository auditLogRepository;
    private final com.exam.system.security.crypto.PasswordCryptoService passwordCryptoService;
    private final ScopeAssembler scopeAssembler;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final NotificationPreferenceService notificationPreferenceService;

    public UserServiceImpl(SysUserRepository userRepository,
                           SysRoleRepository roleRepository,
                           UserScopeRepository userScopeRepository,
                           PasswordHistoryRepository passwordHistoryRepository,
                           UserSecurityPolicyRepository userSecurityPolicyRepository,
                           AuditLogRepository auditLogRepository,
                           com.exam.system.security.crypto.PasswordCryptoService passwordCryptoService,
                           ScopeAssembler scopeAssembler,
                           PasswordPolicyValidator passwordPolicyValidator,
                           NotificationPreferenceService notificationPreferenceService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userScopeRepository = userScopeRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.userSecurityPolicyRepository = userSecurityPolicyRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordCryptoService = passwordCryptoService;
        this.scopeAssembler = scopeAssembler;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.notificationPreferenceService = notificationPreferenceService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserListData listUsers(int page, int size, String role, String status, String search) {
        Specification<SysUser> spec = Specification.where(null);
        if (search != null && !search.isBlank()) {
            String sanitized = InputSanitizer.sanitize(search).toLowerCase();
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("username")), "%" + sanitized + "%"));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), UserStatus.valueOf(status.toUpperCase())));
        }
        if (role != null && !role.isBlank()) {
            spec = spec.and((root, query, cb) -> {
                Join<Object, Object> roles = root.join("roles");
                return cb.equal(roles.get("name"), role);
            });
        }

        Page<SysUser> users = userRepository.findAll(spec, PageRequest.of(Math.max(page - 1, 0), Math.min(size, 100)));
        List<UserResponse> items = users.getContent().stream().map(this::toResponse).collect(Collectors.toList());

        PaginationDto pagination = new PaginationDto();
        pagination.setPage(page);
        pagination.setSize(size);
        pagination.setTotalItems(users.getTotalElements());
        pagination.setTotalPages(users.getTotalPages());

        UserListData data = new UserListData();
        data.setItems(items);
        data.setPagination(pagination);
        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "User not found"));
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String username = InputSanitizer.sanitize(request.getUsername());
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Username already exists");
        }

        passwordPolicyValidator.validate(request.getPassword());

        Set<SysRole> roles = resolveRoles(request.getRoles());

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setSalt(randomSalt());
        user.setPasswordHash(passwordCryptoService.hash(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(roles);
        SysUser saved = userRepository.save(user);

        saveUserScopes(saved.getId(), request.getScopes());
        createPasswordHistory(saved.getId(), saved.getPasswordHash());

        UserSecurityPolicy policy = new UserSecurityPolicy();
        policy.setUserId(saved.getId());
        policy.setConcurrentSessionsAllowed(false);
        userSecurityPolicyRepository.save(policy);

        boolean student = roles.stream().anyMatch(r -> "STUDENT".equalsIgnoreCase(r.getName()));
        if (student) {
            notificationPreferenceService.initializeDefaultsForStudent(saved.getId());
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "User not found"));

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setUsername(InputSanitizer.sanitize(request.getUsername()));
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            user.setStatus(UserStatus.valueOf(request.getStatus()));
            if (UserStatus.ACTIVE.name().equals(request.getStatus())) {
                user.setLockedUntil(null);
            }
        }
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            user.setRoles(resolveRoles(request.getRoles()));
        }
        userRepository.save(user);

        if (request.getScopes() != null) {
            userScopeRepository.deleteByUserId(userId);
            saveUserScopes(userId, request.getScopes());
        }
        return toResponse(user);
    }

    @Override
    @Transactional
    public void disableUser(Long userId) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "User not found"));
        user.setStatus(UserStatus.DISABLED);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserResponse unlockUser(Long userId) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "User not found"));
        user.setStatus(UserStatus.ACTIVE);
        user.setLockedUntil(null);
        userRepository.save(user);

        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction("ACCOUNT_UNLOCKED");
        log.setResource("user");
        log.setResourceId(userId.toString());
        log.setDetailsJson("{\"source\":\"admin\"}");
        auditLogRepository.save(log);

        return toResponse(user);
    }

    @Override
    @Transactional
    public void updateConcurrentSessions(Long userId, ConcurrentSessionsRequest request) {
        UserSecurityPolicy policy = userSecurityPolicyRepository.findById(userId).orElseGet(() -> {
            UserSecurityPolicy created = new UserSecurityPolicy();
            created.setUserId(userId);
            return created;
        });
        policy.setConcurrentSessionsAllowed(request.isAllowed());
        userSecurityPolicyRepository.save(policy);
    }

    private Set<SysRole> resolveRoles(List<String> roleNames) {
        List<SysRole> roles = roleRepository.findByNameIn(roleNames);
        if (roles.size() != roleNames.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                    "One or more roles are invalid");
        }
        return new HashSet<>(roles);
    }

    private void saveUserScopes(Long userId, UserRequestScopeDto scopeRequest) {
        if (scopeRequest == null) {
            return;
        }
        List<Long> gradeIds = scopeRequest.getGradeIds().isEmpty() ? List.of(0L) : scopeRequest.getGradeIds();
        List<Long> classIds = scopeRequest.getClassIds().isEmpty() ? List.of(0L) : scopeRequest.getClassIds();
        List<Long> courseIds = scopeRequest.getCourseIds().isEmpty() ? List.of(0L) : scopeRequest.getCourseIds();
        List<Long> termIds = scopeRequest.getTermId() == null ? List.of(0L) : List.of(scopeRequest.getTermId());

        List<UserScope> scopes = new ArrayList<>();
        for (Long gradeId : gradeIds) {
            for (Long classId : classIds) {
                for (Long courseId : courseIds) {
                    for (Long termId : termIds) {
                        UserScope scope = new UserScope();
                        scope.setUserId(userId);
                        scope.setGradeId(gradeId);
                        scope.setClassId(classId);
                        scope.setCourseId(courseId);
                        scope.setTermId(termId);
                        scopes.add(scope);
                    }
                }
            }
        }
        userScopeRepository.saveAll(scopes);
    }

    private UserResponse toResponse(SysUser user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRoles(user.getRoles().stream().map(SysRole::getName).sorted().collect(Collectors.toList()));
        response.setStatus(user.getStatus().name());
        response.setLockedUntil(user.getLockedUntil());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        ScopeDto scopeDto = scopeAssembler.toDto(userScopeRepository.findByUserId(user.getId()));
        response.setScopes(scopeDto);
        return response;
    }

    private void createPasswordHistory(Long userId, String hash) {
        PasswordHistory history = new PasswordHistory();
        history.setUserId(userId);
        history.setPasswordHash(hash);
        passwordHistoryRepository.save(history);
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
