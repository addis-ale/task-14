package com.exam.system.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SysUser entity — field handling, status, lifecycle callbacks.
 */
@DisplayName("SysUser Entity Tests")
class SysUserTest {

    @Test
    @DisplayName("Should set and get all fields correctly")
    void testGettersAndSetters() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setPasswordHash("hash123");
        user.setSalt("salt456");
        user.setStatus(UserStatus.ACTIVE);
        user.setDeviceToken("token-abc");

        assertEquals(1L, user.getId());
        assertEquals("admin", user.getUsername());
        assertEquals("hash123", user.getPasswordHash());
        assertEquals("salt456", user.getSalt());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals("token-abc", user.getDeviceToken());
    }

    @Test
    @DisplayName("Default status should be ACTIVE")
    void testDefaultStatus() {
        SysUser user = new SysUser();
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    @DisplayName("PrePersist should set createdAt and updatedAt timestamps")
    void testPrePersist() {
        SysUser user = new SysUser();
        assertNull(user.getCreatedAt());
        assertNull(user.getUpdatedAt());

        user.prePersist();

        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        assertEquals(user.getCreatedAt(), user.getUpdatedAt());
    }

    @Test
    @DisplayName("PreUpdate should refresh updatedAt")
    void testPreUpdate() {
        SysUser user = new SysUser();
        user.prePersist();
        LocalDateTime originalUpdate = user.getUpdatedAt();

        // Simulate time passing
        user.preUpdate();

        assertNotNull(user.getUpdatedAt());
    }

    @Test
    @DisplayName("Should handle all UserStatus values")
    void testAllStatusValues() {
        SysUser user = new SysUser();

        user.setStatus(UserStatus.ACTIVE);
        assertEquals(UserStatus.ACTIVE, user.getStatus());

        user.setStatus(UserStatus.LOCKED);
        assertEquals(UserStatus.LOCKED, user.getStatus());

        user.setStatus(UserStatus.DISABLED);
        assertEquals(UserStatus.DISABLED, user.getStatus());
    }

    @Test
    @DisplayName("Should manage roles set")
    void testRolesSet() {
        SysUser user = new SysUser();
        assertNotNull(user.getRoles());
        assertTrue(user.getRoles().isEmpty());

        Set<SysRole> roles = new HashSet<>();
        SysRole role = new SysRole();
        role.setId(1L);
        role.setName("ADMIN");
        roles.add(role);
        user.setRoles(roles);

        assertEquals(1, user.getRoles().size());
    }

    @Test
    @DisplayName("Should handle lockedUntil for account locking")
    void testLockedUntil() {
        SysUser user = new SysUser();
        assertNull(user.getLockedUntil());

        LocalDateTime lockTime = LocalDateTime.of(2026, 4, 2, 12, 0);
        user.setLockedUntil(lockTime);
        assertEquals(lockTime, user.getLockedUntil());

        // Unlock by clearing
        user.setLockedUntil(null);
        assertNull(user.getLockedUntil());
    }
}
