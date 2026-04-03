import { describe, it, expect, vi, beforeEach } from "vitest";
import { createRouter, createWebHistory, type Router } from "vue-router";
import { createPinia, setActivePinia } from "pinia";
import { appRoutes } from "@/router/routes";
import { useAuthStore } from "@/stores/auth";
import type { ActionPermission } from "@/types/auth";

// Mirrors the router guard logic
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

let router: Router;

function makeRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: appRoutes,
  });
}

function setupGuards(r: Router) {
  r.beforeEach((to) => {
    const authStore = useAuthStore();
    const requiresAuth = Boolean(to.meta.requiresAuth);
    const roles = to.meta.roles as string[] | undefined;
    const requiredAction = to.meta.requiredAction as ActionPermission | undefined;

    if (requiresAuth && !authStore.isAuthenticated) {
      return { name: "login", query: { redirect: to.fullPath } };
    }
    if (to.name === "login" && authStore.isAuthenticated) {
      return { path: authStore.homePath };
    }
    if (roles && roles.length > 0 && authStore.activeRole && !roles.includes(authStore.activeRole)) {
      return { name: "forbidden" };
    }
    if (requiredAction && authStore.isAuthenticated && !hasPermission(authStore, requiredAction)) {
      return { name: "forbidden" };
    }
    if (to.path === "/" && authStore.isAuthenticated) {
      return { path: authStore.homePath };
    }
    return true;
  });
}

describe("RBAC Permission Integration Tests", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    router = makeRouter();
    setupGuards(router);
  });

  describe("Action-level route permissions", () => {
    it("ADMIN can access session create route (requires 'create')", async () => {
      const authStore = useAuthStore();
      authStore.token = "test-token";
      authStore.user = { id: 1, username: "admin", roles: ["ADMIN"], activeRole: "ADMIN" };

      await router.push("/scheduling/sessions/new");
      await router.isReady();
      expect(router.currentRoute.value.name).toBe("session-create");
    });

    it("HOMEROOM_TEACHER is blocked from session create (no 'create' permission)", async () => {
      const authStore = useAuthStore();
      authStore.token = "test-token";
      authStore.user = { id: 2, username: "teacher", roles: ["HOMEROOM_TEACHER"], activeRole: "HOMEROOM_TEACHER" };

      await router.push("/scheduling/sessions/new");
      await router.isReady();
      expect(router.currentRoute.value.name).toBe("forbidden");
    });

    it("STUDENT is blocked from roster import route (no 'import' permission)", async () => {
      const authStore = useAuthStore();
      authStore.token = "test-token";
      authStore.user = { id: 3, username: "student", roles: ["STUDENT"], activeRole: "STUDENT" };

      await router.push("/rosters/import");
      await router.isReady();
      expect(router.currentRoute.value.name).toBe("forbidden");
    });

    it("ACADEMIC_AFFAIRS can access compliance reviews (has 'review' permission)", async () => {
      const authStore = useAuthStore();
      authStore.token = "test-token";
      authStore.user = { id: 4, username: "academic", roles: ["ACADEMIC_AFFAIRS"], activeRole: "ACADEMIC_AFFAIRS" };

      await router.push("/notifications/compliance-reviews");
      await router.isReady();
      expect(router.currentRoute.value.name).toBe("compliance-reviews");
    });

    it("SUBJECT_TEACHER is blocked from notification create (no 'create' permission)", async () => {
      const authStore = useAuthStore();
      authStore.token = "test-token";
      authStore.user = { id: 5, username: "subj", roles: ["SUBJECT_TEACHER"], activeRole: "SUBJECT_TEACHER" };

      await router.push("/notifications/create");
      await router.isReady();
      expect(router.currentRoute.value.name).toBe("forbidden");
    });
  });

  describe("Server-provided permission overrides", () => {
    it("user with custom permissions can access routes matching their permissions", async () => {
      const authStore = useAuthStore();
      authStore.token = "test-token";
      authStore.user = {
        id: 6,
        username: "custom",
        roles: ["ACADEMIC_AFFAIRS"],
        activeRole: "ACADEMIC_AFFAIRS",
        permissions: ["view", "create", "import"],
      };

      await router.push("/scheduling/sessions/new");
      await router.isReady();
      expect(router.currentRoute.value.name).toBe("session-create");
    });

    it("user with restricted server permissions is blocked even if role defaults would allow", async () => {
      const authStore = useAuthStore();
      authStore.token = "test-token";
      authStore.user = {
        id: 7,
        username: "restricted",
        roles: ["ADMIN"],
        activeRole: "ADMIN",
        permissions: ["view"], // server restricts to view-only
      };

      await router.push("/scheduling/sessions/new");
      await router.isReady();
      expect(router.currentRoute.value.name).toBe("forbidden");
    });
  });

  describe("Role-based route access matrix", () => {
    const routeTests = [
      { path: "/dashboard", allowedRoles: ["ADMIN", "ACADEMIC_AFFAIRS", "HOMEROOM_TEACHER", "SUBJECT_TEACHER"], deniedRoles: ["STUDENT"] },
      { path: "/admin/users", allowedRoles: ["ADMIN"], deniedRoles: ["ACADEMIC_AFFAIRS", "HOMEROOM_TEACHER", "STUDENT"] },
      { path: "/admin/audit-logs", allowedRoles: ["ADMIN", "ACADEMIC_AFFAIRS"], deniedRoles: ["HOMEROOM_TEACHER", "STUDENT"] },
      { path: "/student/exams", allowedRoles: ["STUDENT"], deniedRoles: ["ADMIN", "ACADEMIC_AFFAIRS"] },
      { path: "/notifications/inbox", allowedRoles: ["STUDENT"], deniedRoles: ["ADMIN", "ACADEMIC_AFFAIRS"] },
    ];

    for (const { path, allowedRoles, deniedRoles } of routeTests) {
      for (const role of allowedRoles) {
        it(`${role} can access ${path}`, async () => {
          const authStore = useAuthStore();
          authStore.token = "test-token";
          authStore.user = { id: 1, username: "user", roles: [role], activeRole: role };

          // Reset router for each sub-test
          const r = makeRouter();
          setupGuards(r);
          await r.push(path);
          await r.isReady();
          expect(r.currentRoute.value.name).not.toBe("forbidden");
          expect(r.currentRoute.value.name).not.toBe("login");
        });
      }

      for (const role of deniedRoles) {
        it(`${role} is denied access to ${path}`, async () => {
          const authStore = useAuthStore();
          authStore.token = "test-token";
          authStore.user = { id: 1, username: "user", roles: [role], activeRole: role };

          const r = makeRouter();
          setupGuards(r);
          await r.push(path);
          await r.isReady();
          expect(r.currentRoute.value.name).toBe("forbidden");
        });
      }
    }
  });

  describe("Unauthenticated access", () => {
    const protectedRoutes = [
      "/dashboard",
      "/admin/users",
      "/scheduling/sessions",
      "/rosters",
      "/student/exams",
      "/notifications",
    ];

    for (const path of protectedRoutes) {
      it(`unauthenticated user is redirected to login from ${path}`, async () => {
        const authStore = useAuthStore();
        expect(authStore.isAuthenticated).toBe(false);

        const r = makeRouter();
        setupGuards(r);
        await r.push(path);
        await r.isReady();
        expect(r.currentRoute.value.name).toBe("login");
        expect(r.currentRoute.value.query.redirect).toBe(path);
      });
    }
  });
});
