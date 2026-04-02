package com.exam.system.controller;

import com.exam.system.dto.ApiResponse;
import com.exam.system.dto.auth.ChangePasswordRequest;
import com.exam.system.dto.auth.LoginRequest;
import com.exam.system.dto.auth.LoginResponse;
import com.exam.system.dto.auth.SwitchRoleRequest;
import com.exam.system.dto.auth.SwitchRoleResponse;
import com.exam.system.dto.auth.UserProfileDto;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ErrorCode;
import com.exam.system.security.rbac.UserContext;
import com.exam.system.security.rbac.UserContextHolder;
import com.exam.system.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication and session APIs under /api/v1/auth.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                            HttpServletRequest servletRequest) {
        String ip = servletRequest.getRemoteAddr();
        String fingerprint = servletRequest.getHeader("User-Agent");
        LoginResponse response = authService.login(request, ip, fingerprint);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(extractToken(authorization));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/active-role")
    public ResponseEntity<ApiResponse<SwitchRoleResponse>> switchRole(@Valid @RequestBody SwitchRoleRequest request,
                                                                      @RequestHeader("Authorization") String authorization) {
        UserContext context = requireContext();
        SwitchRoleResponse response = authService.switchActiveRole(context.getUserId(), extractToken(authorization), request.getRole());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDto>> me(@RequestHeader("Authorization") String authorization) {
        UserContext context = requireContext();
        UserProfileDto data = authService.me(context.getUserId(), extractToken(authorization));
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UserContext context = requireContext();
        authService.changePassword(context.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.successMessage("Password changed successfully"));
    }

    private UserContext requireContext() {
        UserContext context = UserContextHolder.get();
        if (context == null) {
            throw new BusinessException(ErrorCode.SESSION_INVALID, HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return context;
    }

    private String extractToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.SESSION_INVALID, HttpStatus.UNAUTHORIZED,
                    "Missing Bearer token");
        }
        return authorization.substring(7);
    }
}
