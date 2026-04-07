import type { RoleName } from "@/types/ui";

/**
 * Centralized role-action permission matrix.
 * Single source of truth consumed by both router guards and useRBAC composable.
 */
export const ROLE_PERMISSIONS: Record<RoleName, string[]> = {
  ADMIN: ["view", "create", "update", "delete", "import", "export", "review", "publish"],
  ACADEMIC_AFFAIRS: ["view", "create", "update", "import", "export", "review", "publish"],
  HOMEROOM_TEACHER: ["view", "export"],
  SUBJECT_TEACHER: ["view", "export"],
  STUDENT: ["view"],
};

/**
 * Routes accessible per role.
 * Used by router guards and sidebar filtering.
 */
export const ROLE_ROUTES: Record<RoleName, string[]> = {
  ADMIN: [
    "/dashboard", "/admin/users", "/scheduling/sessions", "/rosters",
    "/admin/campus-rooms", "/notifications", "/notifications/compliance-reviews",
    "/notifications/anti-cheat", "/admin/jobs", "/admin/audit-logs",
  ],
  ACADEMIC_AFFAIRS: [
    "/dashboard", "/scheduling/sessions", "/rosters", "/admin/campus-rooms",
    "/notifications", "/notifications/compliance-reviews",
    "/notifications/anti-cheat", "/admin/audit-logs",
  ],
  HOMEROOM_TEACHER: ["/dashboard", "/scheduling/sessions", "/rosters"],
  SUBJECT_TEACHER: ["/dashboard", "/scheduling/sessions", "/rosters"],
  STUDENT: ["/student/exams", "/notifications/inbox", "/student/preferences"],
};
