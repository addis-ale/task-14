import { createRouter, createWebHistory } from "vue-router";
import { appRoutes } from "@/router/routes";
import { useAuthStore } from "@/stores/auth";
import type { ActionPermission } from "@/types/auth";

/** Default permission matrix per role (mirrors useRBAC for guard-level checks) */
const ROLE_PERMISSIONS: Record<string, ActionPermission[]> = {
  ADMIN: ["view", "enter", "import", "review", "publish", "create", "update", "delete", "export", "assign"],
  ACADEMIC_AFFAIRS: ["view", "enter", "import", "review", "publish", "create", "update", "export", "assign"],
  HOMEROOM_TEACHER: ["view", "enter", "export"],
  SUBJECT_TEACHER: ["view", "enter", "export"],
  STUDENT: ["view"],
};

function hasPermission(authStore: ReturnType<typeof useAuthStore>, action: ActionPermission): boolean {
  const perms = authStore.user?.permissions;
  if (perms && perms.length > 0) return perms.includes(action);
  return (ROLE_PERMISSIONS[authStore.activeRole] || []).includes(action);
}

const router = createRouter({
  history: createWebHistory(),
  routes: appRoutes,
});

router.beforeEach((to) => {
  const authStore = useAuthStore();
  const requiresAuth = Boolean(to.meta.requiresAuth);
  const roles = to.meta.roles as string[] | undefined;
  const requiredAction = to.meta.requiredAction as ActionPermission | undefined;

  if (requiresAuth && !authStore.isAuthenticated) {
    return {
      name: "login",
      query: { redirect: to.fullPath },
    };
  }

  if (to.name === "login" && authStore.isAuthenticated) {
    return { path: authStore.homePath };
  }

  if (
    roles &&
    roles.length > 0 &&
    authStore.activeRole &&
    !roles.includes(authStore.activeRole)
  ) {
    return { name: "forbidden" };
  }

  // Action-level permission check for routes that require specific permissions
  if (requiredAction && authStore.isAuthenticated && !hasPermission(authStore, requiredAction)) {
    return { name: "forbidden" };
  }

  if (to.path === "/" && authStore.isAuthenticated) {
    return { path: authStore.homePath };
  }

  return true;
});

router.afterEach((to) => {
  const title = (to.meta.title as string | undefined) || "Secure Exam System";
  document.title = `${title} | Secure Exam System`;
});

export default router;
