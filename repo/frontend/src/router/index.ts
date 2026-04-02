import { createRouter, createWebHistory } from "vue-router";
import { appRoutes } from "@/router/routes";
import { useAuthStore } from "@/stores/auth";

const router = createRouter({
  history: createWebHistory(),
  routes: appRoutes,
});

router.beforeEach((to) => {
  const authStore = useAuthStore();
  const requiresAuth = Boolean(to.meta.requiresAuth);
  const roles = to.meta.roles as string[] | undefined;

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
