import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { storeToRefs } from "pinia";
import { useAuthStore } from "@/stores/auth";

const WARNING_AT_MS = 25 * 60 * 1000;
const EXPIRE_AT_MS = 30 * 60 * 1000;
let watchdogInitialized = false;

export function useAuth() {
  const authStore = useAuthStore();
  const {
    user,
    activeRole,
    isAuthenticated,
    sessionWarningVisible,
    pendingDrafts,
  } = storeToRefs(authStore);
  const lastActivityAt = ref(Date.now());
  let timer: number | undefined;

  const inactivityMinutes = computed(() =>
    Math.floor((Date.now() - lastActivityAt.value) / 60_000),
  );

  async function login(
    username: string,
    password: string,
    remember: boolean,
  ): Promise<void> {
    await authStore.login({ username, password, remember });
  }

  async function logout(): Promise<void> {
    await authStore.logout("MANUAL");
  }

  async function switchRole(role: string): Promise<void> {
    await authStore.switchRole(role);
  }

  function markActivity(): void {
    if (!authStore.isAuthenticated) {
      return;
    }
    lastActivityAt.value = Date.now();
    if (authStore.sessionWarningVisible) {
      authStore.setSessionWarning(false);
    }
  }

  function extendSession(): void {
    markActivity();
    void authStore.fetchProfile();
  }

  function consumeDraftPrompt(): Array<{ formKey: string; updatedAt: string }> {
    const drafts = [...authStore.pendingDrafts];
    authStore.setPendingDrafts([]);
    return drafts;
  }

  function initializeWatchdog(): void {
    if (watchdogInitialized) {
      return;
    }

    watchdogInitialized = true;
    const events: Array<keyof WindowEventMap> = [
      "click",
      "keydown",
      "mousemove",
      "touchstart",
    ];
    events.forEach((event) =>
      window.addEventListener(event, markActivity, { passive: true }),
    );

    timer = window.setInterval(() => {
      if (!authStore.isAuthenticated) {
        return;
      }

      const idle = Date.now() - lastActivityAt.value;
      if (idle >= EXPIRE_AT_MS) {
        void authStore.logout("SESSION_EXPIRED");
        return;
      }

      if (idle >= WARNING_AT_MS && !authStore.sessionWarningVisible) {
        authStore.setSessionWarning(true);
      }
    }, 15_000);
  }

  function teardownWatchdog(): void {
    const events: Array<keyof WindowEventMap> = [
      "click",
      "keydown",
      "mousemove",
      "touchstart",
    ];
    events.forEach((event) => window.removeEventListener(event, markActivity));

    if (timer) {
      window.clearInterval(timer);
      timer = undefined;
    }

    watchdogInitialized = false;
  }

  onMounted(() => {
    initializeWatchdog();
  });

  onBeforeUnmount(() => {
    if (!isAuthenticated.value) {
      teardownWatchdog();
    }
  });

  return {
    user,
    activeRole,
    isAuthenticated,
    sessionWarningVisible,
    inactivityMinutes,
    pendingDrafts,
    login,
    logout,
    switchRole,
    markActivity,
    extendSession,
    consumeDraftPrompt,
    initializeWatchdog,
  };
}
