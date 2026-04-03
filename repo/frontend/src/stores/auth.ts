import { computed, ref } from "vue";
import { defineStore } from "pinia";
import router from "@/router";
import { api, unwrap } from "@/api";
import { logger } from "@/utils/logger";
import { useNotificationsStore } from "@/stores/notifications";
import { useSessionsStore } from "@/stores/sessions";
import { useRostersStore } from "@/stores/rosters";
import type { LoginPayload, UserProfile } from "@/types/auth";

const STORAGE_KEY = "secure-exam-auth";
const REMEMBER_DEVICE_TTL_MS = 7 * 24 * 60 * 60 * 1000; // 7 days

/**
 * Only non-sensitive profile data is persisted to Web Storage.
 * Sensitive auth artifacts (token, sessionSecret) are kept in memory only
 * and are expected to be delivered/managed via HttpOnly secure cookies
 * set by the server on /auth/login and cleared on /auth/logout.
 */
interface PersistedProfile {
  user: UserProfile;
  rememberDevice: boolean;
  loginAt: number;
}

export const useAuthStore = defineStore("auth", () => {
  // Sensitive secrets — memory-only, never persisted to Web Storage
  const token = ref("");
  const sessionSecret = ref("");

  const user = ref<UserProfile | null>(null);
  const rememberDevice = ref(false);
  const loginAt = ref(0);
  const pendingDrafts = ref<Array<{ formKey: string; updatedAt: string }>>([]);
  const sessionWarningVisible = ref(false);

  // Concurrent session detection
  const concurrentSessionDetected = ref(false);
  const activeSessions = ref<Array<{ sessionId: string; device: string; lastActiveAt: string; current: boolean }>>([]);

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

    // Server sets HttpOnly secure cookies for token/sessionSecret.
    // We keep them in memory for the current session's API signing only.
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
    persistProfile();

    // Check for concurrent sessions after successful login
    void checkConcurrentSessions();
  }

  async function fetchProfile(): Promise<void> {
    const profile = await unwrap(api.get<UserProfile>("/auth/me"));
    user.value = profile;
    persistProfile();
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
      persistProfile();
    }

    // Cross-store cleanup: reset data stores to avoid stale data from previous role
    resetDependentStores();
    logger.info("Auth", `Role switched to: ${role}`);
  }

  function resetDependentStores(): void {
    try {
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
      const parsed = JSON.parse(raw) as PersistedProfile;

      // Remember-device TTL: if from localStorage, check 7-day expiry
      if (localRaw && parsed.rememberDevice && parsed.loginAt) {
        const age = Date.now() - parsed.loginAt;
        if (age > REMEMBER_DEVICE_TTL_MS) {
          logger.info("Auth", "Remember-device session expired (>7 days), clearing");
          clearStorage();
          return;
        }
      }

      // Restore non-sensitive profile only.
      // Token/sessionSecret must be re-established via server cookie or re-login.
      user.value = parsed.user;
      rememberDevice.value = parsed.rememberDevice;
      loginAt.value = parsed.loginAt;

      // Attempt to re-establish auth session from server-managed cookie
      reestablishSession();
    } catch {
      clearStorage();
    }
  }

  async function reestablishSession(): Promise<void> {
    try {
      const profile = await unwrap(api.get<UserProfile>("/auth/me"));
      user.value = profile;
      // Server responds with session info if cookie is still valid
      token.value = "cookie-session";
      sessionSecret.value = "cookie-managed";
    } catch {
      // Cookie expired or invalid — require re-login
      user.value = null;
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
    // Clear all auth state including in-memory secrets
    token.value = "";
    sessionSecret.value = "";
    user.value = null;
    pendingDrafts.value = [];
    loginAt.value = 0;
    clearStorage();
    resetDependentStores();

    if (router.currentRoute.value.name !== "login") {
      await router.push({ name: "login" });
    }
  }

  /**
   * Persist only non-sensitive profile data to Web Storage.
   * Token and sessionSecret are NEVER written to storage —
   * they are managed via HttpOnly secure cookies by the server.
   */
  function persistProfile(): void {
    if (!user.value) {
      return;
    }

    const payload: PersistedProfile = {
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

  /**
   * Check for concurrent sessions and populate activeSessions list.
   * Called after login and periodically to detect other active sessions.
   */
  async function checkConcurrentSessions(): Promise<void> {
    try {
      const data = await unwrap(api.get("/auth/sessions"));
      const sessions = data.sessions || data || [];
      activeSessions.value = sessions;
      concurrentSessionDetected.value = sessions.filter(
        (s: { current: boolean }) => !s.current,
      ).length > 0;
    } catch {
      // Non-critical — silently ignore
      concurrentSessionDetected.value = false;
    }
  }

  /**
   * Force-logout all other sessions except the current one.
   */
  async function forceLogoutOtherSessions(): Promise<void> {
    try {
      await unwrap(api.post("/auth/sessions/revoke-others"));
      concurrentSessionDetected.value = false;
      activeSessions.value = activeSessions.value.filter((s) => s.current);
      logger.info("Auth", "Other sessions revoked");
    } catch {
      // silent
    }
  }

  function dismissConcurrentWarning(): void {
    concurrentSessionDetected.value = false;
  }

  return {
    token,
    sessionSecret,
    user,
    rememberDevice,
    loginAt,
    pendingDrafts,
    sessionWarningVisible,
    concurrentSessionDetected,
    activeSessions,
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
    checkConcurrentSessions,
    forceLogoutOtherSessions,
    dismissConcurrentWarning,
  };
});

export type AuthStore = ReturnType<typeof useAuthStore>;
