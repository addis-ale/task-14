import type { RouteRecordRaw } from "vue-router";
import type { RoleName } from "@/types/ui";
import type { ActionPermission } from "@/types/auth";

export interface RouteMetaAuth {
  title: string;
  requiresAuth?: boolean;
  roles?: RoleName[];
  breadcrumb?: string;
  requiredAction?: ActionPermission;
}

export const appRoutes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "login",
    component: () => import("@/views/auth/LoginPage.vue"),
    meta: { title: "登录 Login" },
  },
  {
    path: "/403",
    name: "forbidden",
    component: () => import("@/views/common/ForbiddenPage.vue"),
    meta: { title: "无权限 Forbidden" },
  },
  {
    path: "/",
    component: () => import("@/layouts/MainLayout.vue"),
    meta: { requiresAuth: true },
    children: [
      {
        path: "dashboard",
        name: "dashboard",
        component: () => import("@/views/dashboard/DashboardPage.vue"),
        meta: {
          title: "仪表盘 Dashboard",
          requiresAuth: true,
          roles: [
            "ADMIN",
            "ACADEMIC_AFFAIRS",
            "HOMEROOM_TEACHER",
            "SUBJECT_TEACHER",
          ],
          breadcrumb: "Dashboard",
        },
      },
      {
        path: "scheduling/sessions",
        name: "session-list",
        component: () => import("@/views/scheduling/SessionListPage.vue"),
        meta: {
          title: "考试场次 Session List",
          requiresAuth: true,
          roles: [
            "ADMIN",
            "ACADEMIC_AFFAIRS",
            "HOMEROOM_TEACHER",
            "SUBJECT_TEACHER",
          ],
          breadcrumb: "Scheduling",
        },
      },
      {
        path: "scheduling/sessions/new",
        name: "session-create",
        component: () => import("@/views/scheduling/SessionCreatePage.vue"),
        meta: {
          title: "新建排考 Session Create",
          requiresAuth: true,
          roles: ["ADMIN", "ACADEMIC_AFFAIRS"],
          breadcrumb: "Create Session",
          requiredAction: "create" as const,
        },
      },
      {
        path: "scheduling/sessions/:id",
        name: "session-detail",
        component: () => import("@/views/scheduling/SessionDetailPage.vue"),
        meta: {
          title: "场次详情 Session Detail",
          requiresAuth: true,
          roles: [
            "ADMIN",
            "ACADEMIC_AFFAIRS",
            "HOMEROOM_TEACHER",
            "SUBJECT_TEACHER",
          ],
          breadcrumb: "Session Detail",
        },
      },
      {
        path: "rosters",
        name: "roster-list",
        component: () => import("@/views/rosters/RosterListPage.vue"),
        meta: {
          title: "考生名册 Roster",
          requiresAuth: true,
          roles: [
            "ADMIN",
            "ACADEMIC_AFFAIRS",
            "HOMEROOM_TEACHER",
            "SUBJECT_TEACHER",
          ],
          breadcrumb: "Rosters",
        },
      },
      {
        path: "rosters/import",
        name: "roster-import",
        component: () => import("@/views/rosters/RosterImportPage.vue"),
        meta: {
          title: "批量导入 Import",
          requiresAuth: true,
          roles: [
            "ADMIN",
            "ACADEMIC_AFFAIRS",
            "HOMEROOM_TEACHER",
            "SUBJECT_TEACHER",
          ],
          breadcrumb: "Import",
          requiredAction: "import" as const,
        },
      },
      {
        path: "notifications",
        name: "notification-list",
        component: () =>
          import("@/views/notifications/NotificationListPage.vue"),
        meta: {
          title: "通知管理 Notifications",
          requiresAuth: true,
          roles: ["ADMIN", "ACADEMIC_AFFAIRS"],
          breadcrumb: "Notifications",
        },
      },
      {
        path: "notifications/create",
        name: "notification-create",
        component: () =>
          import("@/views/notifications/NotificationCreatePage.vue"),
        meta: {
          title: "新建通知 Create Notice",
          requiresAuth: true,
          roles: ["ADMIN", "ACADEMIC_AFFAIRS"],
          breadcrumb: "Create Notification",
          requiredAction: "create" as const,
        },
      },
      {
        path: "notifications/compliance-reviews",
        name: "compliance-reviews",
        component: () =>
          import("@/views/notifications/ComplianceReviewPage.vue"),
        meta: {
          title: "合规审核 Compliance Reviews",
          requiresAuth: true,
          roles: ["ADMIN", "ACADEMIC_AFFAIRS"],
          breadcrumb: "Compliance Reviews",
          requiredAction: "review" as const,
        },
      },
      {
        path: "notifications/anti-cheat",
        name: "anti-cheat-review",
        component: () =>
          import("@/views/notifications/AntiCheatReviewPage.vue"),
        meta: {
          title: "反作弊审核 Anti-Cheat Review",
          requiresAuth: true,
          roles: ["ADMIN", "ACADEMIC_AFFAIRS"],
          breadcrumb: "Anti-Cheat Review",
        },
      },
      {
        path: "notifications/inbox",
        name: "inbox",
        component: () => import("@/views/notifications/InboxPage.vue"),
        meta: {
          title: "消息中心 Inbox",
          requiresAuth: true,
          roles: ["STUDENT"],
          breadcrumb: "Inbox",
        },
      },
      {
        path: "admin/users",
        name: "user-management",
        component: () => import("@/views/admin/UserManagementPage.vue"),
        meta: {
          title: "用户管理 Users",
          requiresAuth: true,
          roles: ["ADMIN"],
          breadcrumb: "User Management",
        },
      },
      {
        path: "admin/jobs",
        name: "job-monitor",
        component: () => import("@/views/admin/JobMonitorPage.vue"),
        meta: {
          title: "任务监控 Jobs",
          requiresAuth: true,
          roles: ["ADMIN"],
          breadcrumb: "Job Monitor",
        },
      },
      {
        path: "admin/audit-logs",
        name: "audit-logs",
        component: () => import("@/views/admin/AuditLogPage.vue"),
        meta: {
          title: "审计日志 Audit Logs",
          requiresAuth: true,
          roles: ["ADMIN", "ACADEMIC_AFFAIRS"],
          breadcrumb: "Audit Logs",
        },
      },
      {
        path: "admin/campus-rooms",
        name: "campus-rooms",
        component: () => import("@/views/admin/CampusRoomPage.vue"),
        meta: {
          title: "校区教室 Campus & Rooms",
          requiresAuth: true,
          roles: ["ADMIN", "ACADEMIC_AFFAIRS"],
          breadcrumb: "Campus & Rooms",
        },
      },
      {
        path: "student/exams",
        name: "my-exams",
        component: () => import("@/views/student/MyExamsPage.vue"),
        meta: {
          title: "我的考试 My Exams",
          requiresAuth: true,
          roles: ["STUDENT"],
          breadcrumb: "My Exams",
        },
      },
      {
        path: "student/preferences",
        name: "notification-preferences",
        component: () =>
          import("@/views/student/NotificationPreferencesPage.vue"),
        meta: {
          title: "通知偏好 Preferences",
          requiresAuth: true,
          roles: ["STUDENT"],
          breadcrumb: "Preferences",
        },
      },
      {
        path: "auth/change-password",
        name: "change-password",
        component: () => import("@/views/auth/ChangePasswordPage.vue"),
        meta: {
          title: "修改密码 Change Password",
          requiresAuth: true,
          roles: [
            "ADMIN",
            "ACADEMIC_AFFAIRS",
            "HOMEROOM_TEACHER",
            "SUBJECT_TEACHER",
            "STUDENT",
          ],
          breadcrumb: "Change Password",
        },
      },
    ],
  },
  {
    path: "/:pathMatch(.*)*",
    name: "not-found",
    component: () => import("@/views/common/NotFoundPage.vue"),
    meta: { title: "页面不存在 Not Found" },
  },
];
