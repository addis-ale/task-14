import { computed, ref } from "vue";
import { defineStore } from "pinia";
import router from "@/router";
import { api, unwrap } from "@/api";
import { logger } from "@/utils/logger";
import type { LoginPayload, UserProfile } from "@/types/auth";

const STORAGE_KEY = "secure-exam-auth";
const REMEMBER_DEVICE_TTL_MS = 7 * 24 * 60 * 60 * 1000; // 7 days

interface PersistedAuth {
  token: string;
  sessionSecret: string;
  user: UserProfile;
  rememberDevice: boolean;
  loginAt: number;
}

export const useAuthStore = defineStore("auth", () => {
  const token = ref("");
  const sessionSecret = ref("");
  const user = ref<UserProfile | null>(null);
  const rememberDevice = ref(false);
  const loginAt = ref(0);
  const pendingDrafts = ref<Array<{ formKey: string; updatedAt: string }>>([]);
  const sessionWarningVisible = ref(false);

  const isAuthenticated = computed(() => Boolean(token.value && user.value));
  const activeRole = computed(() => user.value?.activeRole || "");
  const availableRoles = computed(() => user.value?.roles || []);

  const homePath = computed(() => {
    const role = activeRole.value;
    if (role === "STUDENT") {
      return "/student/exams";
    }
    return "/dashboard";
  });

  async function login(input: {
    username: string;
    password: string;
    remember: boolean;
  }): Promise<void> {
    const payload = await unwrap(
      api.post<LoginPayload>("/auth/login", {
        username: input.username,
        password: input.password,
      }),
    );

    token.value = payload.token;
    sessionSecret.value = payload.sessionSecret || payload.token;
    user.value = payload.user;
    rememberDevice.value = input.remember;
    loginAt.value = Date.now();
    pendingDrafts.value = payload.drafts || [];

    if (pendingDrafts.value.length === 0) {
      try {
        const draftResult = await unwrap(
          api.get("/drafts", {
            params: { page: 1, size: 20 },
          }),
        );
        pendingDrafts.value = draftResult.items || draftResult || [];
      } catch {
        pendingDrafts.value = [];
      }
    }

    logger.info("Auth", `User logged in: ${payload.user.username}`);
    persistSession();
  }

  async function fetchProfile(): Promise<void> {
    const profile = await unwrap(api.get<UserProfile>("/auth/me"));
    user.value = profile;
    persistSession();
  }

  async function switchRole(role: string): Promise<void> {
    const data = await unwrap(
      api.put<{ activeRole: string; scopes?: unknown }>("/auth/active-role", {
        role,
      }),
    );
    if (user.value) {
      user.value = {
        ...user.value,
        activeRole: data.activeRole,
        scopes: (data.scopes as UserProfile["scopes"]) || user.value.scopes,
      };
      persistSession();
    }

    // Cross-store cleanup: reset data stores to avoid stale data from previous role
    resetDependentStores();
    logger.info("Auth", `Role switched to: ${role}`);
  }

  function resetDependentStores(): void {
    try {
      const { useNotificationsStore } = require("@/stores/notifications");
      const { useSessionsStore } = require("@/stores/sessions");
      const { useRostersStore } = require("@/stores/rosters");

      const notifStore = useNotificationsStore();
      notifStore.inbox = null;
      notifStore.unread = 0;

      const sessionsStore = useSessionsStore();
      sessionsStore.sessions = null;

      const rostersStore = useRostersStore();
      rostersStore.rows = null;
    } catch {
      // Stores may not be initialized yet — safe to ignore
    }
  }

  function restoreSession(): void {
    const localRaw = localStorage.getItem(STORAGE_KEY);
    const sessionRaw = sessionStorage.getItem(STORAGE_KEY);
    const raw = localRaw || sessionRaw;
    if (!raw) {
      return;
    }

    try {
      const parsed = JSON.parse(raw) as PersistedAuth;

      // Remember-device TTL: if from localStorage, check 7-day expiry
      if (localRaw && parsed.rememberDevice && parsed.loginAt) {
        const age = Date.now() - parsed.loginAt;
        if (age > REMEMBER_DEVICE_TTL_MS) {
          logger.info("Auth", "Remember-device session expired (>7 days), clearing");
          clearStorage();
          return;
        }
      }

      token.value = parsed.token;
      sessionSecret.value = parsed.sessionSecret;
      user.value = parsed.user;
      rememberDevice.value = parsed.rememberDevice;
      loginAt.value = parsed.loginAt;
    } catch {
      clearStorage();
    }
  }

  function setPendingDrafts(
    drafts: Array<{ formKey: string; updatedAt: string }>,
  ): void {
    pendingDrafts.value = drafts;
  }

  function setSessionWarning(visible: boolean): void {
    sessionWarningVisible.value = visible;
  }

  async function logout(
    reason: "MANUAL" | "SESSION_EXPIRED" = "MANUAL",
  ): Promise<void> {
    if (token.value && reason === "MANUAL") {
      try {
        await api.post("/auth/logout");
      } catch {
        // no-op
      }
    }

    logger.info("Auth", `Logout: ${reason}`);
    token.value = "";
    sessionSecret.value = "";
    user.value = null;
    pendingDrafts.value = [];
    loginAt.value = 0;
    clearStorage();

    if (router.currentRoute.value.name !== "login") {
      await router.push({ name: "login" });
    }
  }

  function persistSession(): void {
    if (!token.value || !user.value) {
      return;
    }

    const payload: PersistedAuth = {
      token: token.value,
      sessionSecret: sessionSecret.value,
      user: user.value,
      rememberDevice: rememberDevice.value,
      loginAt: loginAt.value,
    };

    const raw = JSON.stringify(payload);
    if (rememberDevice.value) {
      localStorage.setItem(STORAGE_KEY, raw);
      sessionStorage.removeItem(STORAGE_KEY);
      return;
    }

    sessionStorage.setItem(STORAGE_KEY, raw);
    localStorage.removeItem(STORAGE_KEY);
  }

  function clearStorage(): void {
    localStorage.removeItem(STORAGE_KEY);
    sessionStorage.removeItem(STORAGE_KEY);
  }

  /**
   * Validates a redirect path for safety (no open redirect).
   * Must start with "/" and must not contain "://" or other external patterns.
   */
  function validateRedirect(path: string | null | undefined): string {
    if (!path || typeof path !== "string") return homePath.value;
    if (!path.startsWith("/")) return homePath.value;
    if (path.includes("://")) return homePath.value;
    if (path.includes("\\")) return homePath.value;
    if (path.startsWith("//")) return homePath.value;
    return path;
  }

  return {
    token,
    sessionSecret,
    user,
    rememberDevice,
    loginAt,
    pendingDrafts,
    sessionWarningVisible,
    isAuthenticated,
    activeRole,
    availableRoles,
    homePath,
    login,
    logout,
    switchRole,
    fetchProfile,
    restoreSession,
    setPendingDrafts,
    setSessionWarning,
    validateRedirect,
  };
});

export type AuthStore = ReturnType<typeof useAuthStore>;
