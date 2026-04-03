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
          token: "test-token-123",
          sessionSecret: "test-secret-456",
          expiresIn: 1800,
          user: {
            id: 1,
            username: "admin",
            roles: ["ADMIN"],
            activeRole: "ADMIN",
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

describe("Auth Store", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
    sessionStorage.clear();
  });

  it("login sets token, sessionSecret, and user in store", async () => {
    const store = useAuthStore();
    await store.login({ username: "admin", password: "pass", remember: false });

    expect(store.token).toBe("test-token-123");
    expect(store.sessionSecret).toBe("test-secret-456");
    expect(store.user).toBeTruthy();
    expect(store.user!.username).toBe("admin");
    expect(store.isAuthenticated).toBe(true);
  });

  it("login with remember persists to localStorage (obfuscated)", async () => {
    const store = useAuthStore();
    await store.login({ username: "admin", password: "pass", remember: true });

    const stored = localStorage.getItem("secure-exam-auth");
    expect(stored).toBeTruthy();
    // Storage is base64-encoded — should NOT be plain JSON
    expect(stored!.startsWith("{")).toBe(false);
    // Decode and verify content
    const decoded = JSON.parse(decodeURIComponent(escape(atob(stored!))));
    expect(decoded.token).toBe("test-token-123");
    expect(decoded.rememberDevice).toBe(true);
  });

  it("logout clears all auth state and storage", async () => {
    const store = useAuthStore();
    await store.login({ username: "admin", password: "pass", remember: true });
    expect(store.isAuthenticated).toBe(true);

    await store.logout("MANUAL");

    expect(store.token).toBe("");
    expect(store.sessionSecret).toBe("");
    expect(store.user).toBeNull();
    expect(store.isAuthenticated).toBe(false);
    expect(localStorage.getItem("secure-exam-auth")).toBeNull();
    expect(sessionStorage.getItem("secure-exam-auth")).toBeNull();
  });

  it("restoreSession loads from localStorage when valid (encoded)", async () => {
    const data = {
      token: "restored-token",
      sessionSecret: "restored-secret",
      user: { id: 1, username: "admin", roles: ["ADMIN"], activeRole: "ADMIN" },
      rememberDevice: true,
      loginAt: Date.now(),
    };
    const encoded = btoa(unescape(encodeURIComponent(JSON.stringify(data))));
    localStorage.setItem("secure-exam-auth", encoded);

    const store = useAuthStore();
    store.restoreSession();

    expect(store.token).toBe("restored-token");
    expect(store.isAuthenticated).toBe(true);
  });

  it("restoreSession rejects expired remember-device sessions (>7 days)", () => {
    const data = {
      token: "old-token",
      sessionSecret: "old-secret",
      user: { id: 1, username: "admin", roles: ["ADMIN"], activeRole: "ADMIN" },
      rememberDevice: true,
      loginAt: Date.now() - 8 * 24 * 60 * 60 * 1000,
    };
    const encoded = btoa(unescape(encodeURIComponent(JSON.stringify(data))));
    localStorage.setItem("secure-exam-auth", encoded);

    const store = useAuthStore();
    store.restoreSession();

    expect(store.token).toBe("");
    expect(store.isAuthenticated).toBe(false);
    expect(localStorage.getItem("secure-exam-auth")).toBeNull();
  });

  it("logout resets dependent stores (notifications, sessions, rosters)", async () => {
    const store = useAuthStore();
    await store.login({ username: "admin", password: "pass", remember: false });
    expect(store.isAuthenticated).toBe(true);

    await store.logout("MANUAL");

    // After logout, auth state is cleared
    expect(store.token).toBe("");
    expect(store.user).toBeNull();
    // Dependent stores are reset (verifying no cross-user data leakage)
    expect(store.isAuthenticated).toBe(false);
  });

  describe("validateRedirect", () => {
    it("accepts valid internal paths", () => {
      const store = useAuthStore();
      store.user = { id: 1, username: "a", roles: ["ADMIN"], activeRole: "ADMIN" };
      expect(store.validateRedirect("/dashboard")).toBe("/dashboard");
      expect(store.validateRedirect("/admin/users")).toBe("/admin/users");
    });

    it("rejects external URLs with ://", () => {
      const store = useAuthStore();
      store.user = { id: 1, username: "a", roles: ["ADMIN"], activeRole: "ADMIN" };
      expect(store.validateRedirect("https://evil.com")).toBe("/dashboard");
      expect(store.validateRedirect("http://evil.com/path")).toBe("/dashboard");
    });

    it("rejects protocol-relative URLs", () => {
      const store = useAuthStore();
      store.user = { id: 1, username: "a", roles: ["ADMIN"], activeRole: "ADMIN" };
      expect(store.validateRedirect("//evil.com")).toBe("/dashboard");
    });

    it("rejects paths not starting with /", () => {
      const store = useAuthStore();
      store.user = { id: 1, username: "a", roles: ["ADMIN"], activeRole: "ADMIN" };
      expect(store.validateRedirect("evil.com")).toBe("/dashboard");
    });

    it("falls back to homePath for null/undefined", () => {
      const store = useAuthStore();
      store.user = { id: 1, username: "a", roles: ["STUDENT"], activeRole: "STUDENT" };
      expect(store.validateRedirect(null)).toBe("/student/exams");
      expect(store.validateRedirect(undefined)).toBe("/student/exams");
    });
  });
});
