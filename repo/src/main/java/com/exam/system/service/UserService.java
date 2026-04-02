package com.exam.system.service;

import com.exam.system.dto.user.ConcurrentSessionsRequest;
import com.exam.system.dto.user.CreateUserRequest;
import com.exam.system.dto.user.UpdateUserRequest;
import com.exam.system.dto.user.UserListData;
import com.exam.system.dto.user.UserResponse;

public interface UserService {

    /**
     * Lists users with pagination and optional filters.
     */
    UserListData listUsers(int page, int size, String role, String status, String search);

    /**
     * Returns one user by ID.
     */
    UserResponse getUser(Long userId);

    /**
     * Creates a user with assigned roles and ABAC scopes.
     */
    UserResponse createUser(CreateUserRequest request);

    /**
     * Updates mutable user fields, roles, and scopes.
     */
    UserResponse updateUser(Long userId, UpdateUserRequest request);

    /**
     * Soft deletes a user by setting DISABLED status.
     */
    void disableUser(Long userId);

    /**
     * Clears lock state for a user account.
     */
    UserResponse unlockUser(Long userId);

    /**
     * Enables or disables concurrent session policy for one user.
     */
    void updateConcurrentSessions(Long userId, ConcurrentSessionsRequest request);
}
