package com.exam.system.controller;

import com.exam.system.dto.ApiResponse;
import com.exam.system.dto.user.ConcurrentSessionsRequest;
import com.exam.system.dto.user.CreateUserRequest;
import com.exam.system.dto.user.UpdateUserRequest;
import com.exam.system.dto.user.UserListData;
import com.exam.system.dto.user.UserResponse;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ErrorCode;
import com.exam.system.security.rbac.UserContext;
import com.exam.system.security.rbac.UserContextHolder;
import com.exam.system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserListData>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        requireAdmin();
        return ResponseEntity.ok(ApiResponse.success(userService.listUsers(page, size, role, status, search)));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long userId) {
        requireAdmin();
        return ResponseEntity.ok(ApiResponse.success(userService.getUser(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        requireAdmin();
        return ResponseEntity.status(201).body(ApiResponse.success(userService.createUser(request)));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable Long userId,
                                                                @Valid @RequestBody UpdateUserRequest request) {
        requireAdmin();
        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(userId, request)));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> disableUser(@PathVariable Long userId) {
        requireAdmin();
        userService.disableUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/unlock")
    public ResponseEntity<ApiResponse<UserResponse>> unlockUser(@PathVariable Long userId) {
        requireAdmin();
        return ResponseEntity.ok(ApiResponse.success(userService.unlockUser(userId)));
    }

    @PutMapping("/{userId}/concurrent-sessions")
    public ResponseEntity<ApiResponse<Void>> updateConcurrentSessions(@PathVariable Long userId,
                                                                      @Valid @RequestBody ConcurrentSessionsRequest request) {
        requireAdmin();
        userService.updateConcurrentSessions(userId, request);
        return ResponseEntity.ok(ApiResponse.successMessage("Concurrent session policy updated"));
    }

    private UserContext context() {
        return UserContextHolder.get();
    }

    private void requireAdmin() {
        UserContext context = context();
        if (context == null || !"ADMIN".equalsIgnoreCase(context.getActiveRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Admin role required");
        }
    }
}
