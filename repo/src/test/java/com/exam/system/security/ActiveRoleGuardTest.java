package com.exam.system.security;

import com.exam.system.exception.BusinessException;
import com.exam.system.security.rbac.ActiveRoleGuard;
import com.exam.system.security.rbac.UserContext;
import com.exam.system.security.rbac.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ActiveRoleGuardTest {

    private final ActiveRoleGuard guard = new ActiveRoleGuard();

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("requireAny passes when user has allowed role")
    void requireAnyPassesWithAllowedRole() {
        setUserContext("ADMIN");
        assertThatCode(() -> guard.requireAny("ADMIN", "ACADEMIC_AFFAIRS"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("requireAny throws when user has wrong role")
    void requireAnyThrowsWithWrongRole() {
        setUserContext("STUDENT");
        assertThatThrownBy(() -> guard.requireAny("ADMIN", "ACADEMIC_AFFAIRS"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("requireAny throws when no user context")
    void requireAnyThrowsWithNoContext() {
        assertThatThrownBy(() -> guard.requireAny("ADMIN"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("requireAny is case-insensitive")
    void requireAnyIsCaseInsensitive() {
        setUserContext("admin");
        assertThatCode(() -> guard.requireAny("ADMIN"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("isAny returns true for matching role")
    void isAnyReturnsTrueForMatchingRole() {
        setUserContext("STUDENT");
        assertThat(guard.isAny("STUDENT")).isTrue();
    }

    @Test
    @DisplayName("isAny returns false for non-matching role")
    void isAnyReturnsFalseForNonMatchingRole() {
        setUserContext("STUDENT");
        assertThat(guard.isAny("ADMIN")).isFalse();
    }

    @Test
    @DisplayName("isAny returns false when no context")
    void isAnyReturnsFalseWhenNoContext() {
        assertThat(guard.isAny("ADMIN")).isFalse();
    }

    private void setUserContext(String role) {
        UserContext ctx = new UserContext();
        ctx.setUserId(1L);
        ctx.setUsername("testuser");
        ctx.setActiveRole(role);
        UserContextHolder.set(ctx);
    }
}
