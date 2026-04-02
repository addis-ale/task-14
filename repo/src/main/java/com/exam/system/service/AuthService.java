package com.exam.system.service;

import com.exam.system.dto.auth.ChangePasswordRequest;
import com.exam.system.dto.auth.LoginRequest;
import com.exam.system.dto.auth.LoginResponse;
import com.exam.system.dto.auth.SwitchRoleResponse;
import com.exam.system.dto.auth.UserProfileDto;

public interface AuthService {

    /**
     * Authenticates a user and creates a new server-side session.
     */
    LoginResponse login(LoginRequest request, String ipAddress, String fallbackFingerprint);

    /**
     * Invalidates an existing session token.
     */
    void logout(String token);

    /**
     * Switches active role for multi-role accounts.
     */
    SwitchRoleResponse switchActiveRole(Long userId, String token, String role);

    /**
     * Returns details for the currently authenticated session user.
     */
    UserProfileDto me(Long userId, String token);

    /**
     * Changes current user password after policy and history checks.
     */
    void changePassword(Long userId, ChangePasswordRequest request);
}
