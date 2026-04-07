package com.exam.system.controller;

import com.exam.system.exception.BusinessException;
import com.exam.system.security.rbac.ActiveRoleGuard;
import com.exam.system.security.rbac.UserContext;
import com.exam.system.security.rbac.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for authorization enforcement across controller endpoints.
 * Verifies that ActiveRoleGuard correctly rejects unauthorized access and passes authorized requests.
 */
class AuthorizationIntegrationTest {

    private final ActiveRoleGuard guard = new ActiveRoleGuard();

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Nested
    @DisplayName("Admin-only endpoints")
    class AdminEndpoints {

        @Test
        @DisplayName("Admin role can access user management")
        void adminCanAccessUserManagement() {
            setContext("ADMIN");
            assertThatCode(() -> guard.requireAny("ADMIN"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Student cannot access user management (403)")
        void studentCannotAccessUserManagement() {
            setContext("STUDENT");
            assertThatThrownBy(() -> guard.requireAny("ADMIN"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Homeroom teacher cannot access user management (403)")
        void teacherCannotAccessUserManagement() {
            setContext("HOMEROOM_TEACHER");
            assertThatThrownBy(() -> guard.requireAny("ADMIN"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("No context results in 403")
        void noContextDenied() {
            assertThatThrownBy(() -> guard.requireAny("ADMIN"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("Admin + Academic Affairs endpoints")
    class AdminAcademicEndpoints {

        @Test
        @DisplayName("Admin can access scheduling")
        void adminCanAccessScheduling() {
            setContext("ADMIN");
            assertThatCode(() -> guard.requireAny("ADMIN", "ACADEMIC_AFFAIRS"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Academic affairs can access scheduling")
        void academicAffairsCanAccessScheduling() {
            setContext("ACADEMIC_AFFAIRS");
            assertThatCode(() -> guard.requireAny("ADMIN", "ACADEMIC_AFFAIRS"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Student cannot access scheduling create (403)")
        void studentCannotCreateSession() {
            setContext("STUDENT");
            assertThatThrownBy(() -> guard.requireAny("ADMIN", "ACADEMIC_AFFAIRS"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Homeroom teacher cannot access scheduling create (403)")
        void teacherCannotCreateSession() {
            setContext("HOMEROOM_TEACHER");
            assertThatThrownBy(() -> guard.requireAny("ADMIN", "ACADEMIC_AFFAIRS"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("Staff read endpoints (all staff roles)")
    class StaffReadEndpoints {

        @Test
        @DisplayName("All staff roles can access session list")
        void allStaffCanListSessions() {
            for (String role : new String[]{"ADMIN", "ACADEMIC_AFFAIRS", "HOMEROOM_TEACHER", "SUBJECT_TEACHER"}) {
                setContext(role);
                assertThatCode(() -> guard.requireAny("ADMIN", "ACADEMIC_AFFAIRS", "HOMEROOM_TEACHER", "SUBJECT_TEACHER"))
                        .doesNotThrowAnyException();
                UserContextHolder.clear();
            }
        }

        @Test
        @DisplayName("Student cannot access session list (403)")
        void studentCannotListSessions() {
            setContext("STUDENT");
            assertThatThrownBy(() -> guard.requireAny("ADMIN", "ACADEMIC_AFFAIRS", "HOMEROOM_TEACHER", "SUBJECT_TEACHER"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("Student-only endpoints")
    class StudentEndpoints {

        @Test
        @DisplayName("Student can access inbox")
        void studentCanAccessInbox() {
            setContext("STUDENT");
            assertThatCode(() -> guard.requireAny("STUDENT"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Admin cannot access student inbox (403)")
        void adminCannotAccessStudentInbox() {
            setContext("ADMIN");
            assertThatThrownBy(() -> guard.requireAny("STUDENT"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Teacher cannot access student inbox (403)")
        void teacherCannotAccessStudentInbox() {
            setContext("HOMEROOM_TEACHER");
            assertThatThrownBy(() -> guard.requireAny("STUDENT"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("Compliance review endpoints")
    class ComplianceEndpoints {

        @Test
        @DisplayName("Admin can access compliance reviews")
        void adminCanAccessComplianceReviews() {
            setContext("ADMIN");
            assertThatCode(() -> guard.requireAny("ADMIN", "ACADEMIC_AFFAIRS"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Academic affairs can access compliance reviews")
        void academicAffairsCanAccessComplianceReviews() {
            setContext("ACADEMIC_AFFAIRS");
            assertThatCode(() -> guard.requireAny("ADMIN", "ACADEMIC_AFFAIRS"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Subject teacher cannot access compliance reviews (403)")
        void subjectTeacherCannotAccessComplianceReviews() {
            setContext("SUBJECT_TEACHER");
            assertThatThrownBy(() -> guard.requireAny("ADMIN", "ACADEMIC_AFFAIRS"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    private void setContext(String role) {
        UserContext ctx = new UserContext();
        ctx.setUserId(1L);
        ctx.setUsername("testuser");
        ctx.setActiveRole(role);
        UserContextHolder.set(ctx);
    }
}
