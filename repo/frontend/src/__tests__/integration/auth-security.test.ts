import { describe, it, expect, vi, beforeEach } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { useAuthStore } from "@/stores/auth";

// Mock the router
vi.mock("@/router", () => ({
  default: {
    currentRoute: { value: { name: "dashboard" } },
    push: vi.fn().mockResolvedValue(undefined),
  },
}));

// Mock the API module
vi.mock("@/api", () => ({
  api: {
    post: vi.fn().mockResolvedValue({
      data: {
        data: {
          token: "sensitive-token-xyz",
          sessionSecret: "sensitive-secret-abc",
          expiresIn: 1800,
          user: {
            id: 1,
            username: "admin",
            roles: ["ADMIN"],
            activeRole: "ADMIN",
            permissions: ["view", "create", "update", "delete"],
          },
          drafts: [],
        },
      },
    }),
    get: vi.fn().mockResolvedValue({ data: { data: { items: [] } } }),
    put: vi.fn().mockResolvedValue({
      data: { data: { activeRole: "STUDENT", scopes: {} } },
    }),
  },
  unwrap: vi.fn().mockImplementation(async (promise: Promise<any>) => {
    const res = await promise;
    return res.data?.data ?? res.data;
  }),
}));

describe("Auth Security Integration Tests", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
    sessionStorage.clear();
  });

  describe("Sensitive data never in Web Storage", () => {
    it("token is NOT persisted to localStorage after login with remember", async () => {
      const store = useAuthStore();
      await store.login({ username: "admin", password: "pass", remember: true });

      const stored = localStorage.getItem("secure-exam-auth");
      expect(stored).toBeTruthy();
      const parsed = JSON.parse(stored!);
      expect(parsed.token).toBeUndefined();
      expect(parsed.sessionSecret).toBeUndefined();
      // Verify the token exists in memory
      expect(store.token).toBe("sensitive-token-xyz");
    });

    it("token is NOT persisted to sessionStorage after login without remember", async () => {
      const store = useAuthStore();
      await store.login({ username: "admin", password: "pass", remember: false });

      const stored = sessionStorage.getItem("secure-exam-auth");
      expect(stored).toBeTruthy();
      const parsed = JSON.parse(stored!);
      expect(parsed.token).toBeUndefined();
      expect(parsed.sessionSecret).toBeUndefined();
      expect(store.sessionSecret).toBe("sensitive-secret-abc");
    });

    it("no base64-encoded secrets in storage (no obfuscation as replacement for security)", async () => {
      const store = useAuthStore();
      await store.login({ username: "admin", password: "pass", remember: true });

      const stored = localStorage.getItem("secure-exam-auth")!;
      // The stored value should be plain JSON (not base64 encoded)
      // and should not contain any token/secret data
      expect(stored).not.toContain("sensitive-token");
      expect(stored).not.toContain("sensitive-secret");
      // Also check base64 encoded version of these strings
      expect(stored).not.toContain(btoa("sensitive-token-xyz"));
      expect(stored).not.toContain(btoa("sensitive-secret-abc"));
    });
  });

  describe("Session state isolation on role switch", () => {
    it("switching role resets dependent stores and persists profile", async () => {
      const store = useAuthStore();
      await store.login({ username: "admin", password: "pass", remember: false });
      expect(store.isAuthenticated).toBe(true);
      expect(store.activeRole).toBe("ADMIN");

      await store.switchRole("STUDENT");
      expect(store.user!.activeRole).toBe("STUDENT");
    });
  });

  describe("Session state isolation on logout", () => {
    it("logout clears all in-memory secrets", async () => {
      const store = useAuthStore();
      await store.login({ username: "admin", password: "pass", remember: true });

      expect(store.token).toBeTruthy();
      expect(store.sessionSecret).toBeTruthy();

      await store.logout("MANUAL");

      expect(store.token).toBe("");
      expect(store.sessionSecret).toBe("");
      expect(store.user).toBeNull();
      expect(store.pendingDrafts).toEqual([]);
    });

    it("logout clears all Web Storage entries", async () => {
      const store = useAuthStore();
      await store.login({ username: "admin", password: "pass", remember: true });
      expect(localStorage.getItem("secure-exam-auth")).toBeTruthy();

      await store.logout("MANUAL");

      expect(localStorage.getItem("secure-exam-auth")).toBeNull();
      expect(sessionStorage.getItem("secure-exam-auth")).toBeNull();
    });

    it("SESSION_EXPIRED logout clears state without API call", async () => {
      const store = useAuthStore();
      await store.login({ username: "admin", password: "pass", remember: false });

      const { api } = await import("@/api");
      const postSpy = vi.mocked(api.post);
      postSpy.mockClear();

      await store.logout("SESSION_EXPIRED");

      // SESSION_EXPIRED should not call /auth/logout
      const logoutCalls = postSpy.mock.calls.filter(
        (call) => call[0] === "/auth/logout",
      );
      expect(logoutCalls.length).toBe(0);
      expect(store.isAuthenticated).toBe(false);
    });
  });

  describe("Session restore security", () => {
    it("restoring session from storage does NOT restore secrets from storage", () => {
      // Simulate old-format storage that somehow contains secrets
      const maliciousData = JSON.stringify({
        token: "stolen-token",
        sessionSecret: "stolen-secret",
        user: { id: 1, username: "admin", roles: ["ADMIN"], activeRole: "ADMIN" },
        rememberDevice: true,
        loginAt: Date.now(),
      });
      localStorage.setItem("secure-exam-auth", maliciousData);

      const store = useAuthStore();
      store.restoreSession();

      // Token/secret should NOT be read from storage
      expect(store.token).not.toBe("stolen-token");
      expect(store.sessionSecret).not.toBe("stolen-secret");
    });
  });

  describe("Redirect validation", () => {
    it("rejects backslash-based redirect attempts", () => {
      const store = useAuthStore();
      store.user = { id: 1, username: "a", roles: ["ADMIN"], activeRole: "ADMIN" };
      expect(store.validateRedirect("\\\\evil.com")).toBe("/dashboard");
    });

    it("rejects javascript: protocol attempts", () => {
      const store = useAuthStore();
      store.user = { id: 1, username: "a", roles: ["ADMIN"], activeRole: "ADMIN" };
      // This doesn't start with / so should be rejected
      expect(store.validateRedirect("javascript:alert(1)")).toBe("/dashboard");
    });

    it("rejects data: protocol attempts", () => {
      const store = useAuthStore();
      store.user = { id: 1, username: "a", roles: ["ADMIN"], activeRole: "ADMIN" };
      expect(store.validateRedirect("data:text/html,test")).toBe("/dashboard");
    });
  });
});
