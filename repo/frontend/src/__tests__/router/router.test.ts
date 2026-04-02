import { describe, it, expect, beforeEach } from "vitest";
import { createRouter, createWebHistory, type Router } from "vue-router";
import { createPinia, setActivePinia } from "pinia";
import { appRoutes } from "@/router/routes";
import { useAuthStore } from "@/stores/auth";

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

    if (requiresAuth && !authStore.isAuthenticated) {
      return { name: "login", query: { redirect: to.fullPath } };
    }
    if (to.name === "login" && authStore.isAuthenticated) {
      return { path: authStore.homePath };
    }
    if (roles && roles.length > 0 && authStore.activeRole && !roles.includes(authStore.activeRole)) {
      return { name: "forbidden" };
    }
    if (to.path === "/" && authStore.isAuthenticated) {
      return { path: authStore.homePath };
    }
    return true;
  });
}

describe("Router Guards", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    router = makeRouter();
    setupGuards(router);
  });

  it("redirects unauthenticated user to /login with redirect query", async () => {
    const authStore = useAuthStore();
    // Not authenticated (default state)
    expect(authStore.isAuthenticated).toBe(false);

    await router.push("/dashboard");
    await router.isReady();
    expect(router.currentRoute.value.name).toBe("login");
    expect(router.currentRoute.value.query.redirect).toBe("/dashboard");
  });

  it("redirects authenticated user with wrong role to /403", async () => {
    const authStore = useAuthStore();
    authStore.token = "test-token";
    authStore.user = {
      id: 1,
      username: "student1",
      roles: ["STUDENT"],
      activeRole: "STUDENT",
    };

    await router.push("/admin/users"); // ADMIN only
    await router.isReady();
    expect(router.currentRoute.value.name).toBe("forbidden");
  });

  it("redirects authenticated user accessing /login to homePath", async () => {
    const authStore = useAuthStore();
    authStore.token = "test-token";
    authStore.user = {
      id: 1,
      username: "admin",
      roles: ["ADMIN"],
      activeRole: "ADMIN",
    };

    await router.push("/login");
    await router.isReady();
    expect(router.currentRoute.value.path).toBe("/dashboard");
  });

  it("resolves STUDENT homePath to /student/exams", () => {
    const authStore = useAuthStore();
    authStore.user = {
      id: 1,
      username: "s1",
      roles: ["STUDENT"],
      activeRole: "STUDENT",
    };
    expect(authStore.homePath).toBe("/student/exams");
  });

  it("resolves non-STUDENT homePath to /dashboard", () => {
    const authStore = useAuthStore();
    authStore.user = {
      id: 1,
      username: "admin",
      roles: ["ADMIN"],
      activeRole: "ADMIN",
    };
    expect(authStore.homePath).toBe("/dashboard");
  });

  it("resolves ACADEMIC_AFFAIRS homePath to /dashboard", () => {
    const authStore = useAuthStore();
    authStore.user = {
      id: 2,
      username: "academic",
      roles: ["ACADEMIC_AFFAIRS"],
      activeRole: "ACADEMIC_AFFAIRS",
    };
    expect(authStore.homePath).toBe("/dashboard");
  });
});
