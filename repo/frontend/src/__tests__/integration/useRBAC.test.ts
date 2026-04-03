import { describe, it, expect, beforeEach } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { useAuthStore } from "@/stores/auth";
import { useRBAC } from "@/composables/useRBAC";

describe("useRBAC composable", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  describe("Default role-based permissions", () => {
    it("ADMIN has all action permissions", () => {
      const authStore = useAuthStore();
      authStore.token = "t";
      authStore.user = { id: 1, username: "admin", roles: ["ADMIN"], activeRole: "ADMIN" };

      const { can, permissions } = useRBAC();

      expect(can("view")).toBe(true);
      expect(can("create")).toBe(true);
      expect(can("update")).toBe(true);
      expect(can("delete")).toBe(true);
      expect(can("import")).toBe(true);
      expect(can("review")).toBe(true);
      expect(can("publish")).toBe(true);
      expect(can("export")).toBe(true);
      expect(can("assign")).toBe(true);
      expect(permissions.value.length).toBe(10);
    });

    it("STUDENT has only view permission", () => {
      const authStore = useAuthStore();
      authStore.token = "t";
      authStore.user = { id: 2, username: "student", roles: ["STUDENT"], activeRole: "STUDENT" };

      const { can } = useRBAC();

      expect(can("view")).toBe(true);
      expect(can("create")).toBe(false);
      expect(can("import")).toBe(false);
      expect(can("publish")).toBe(false);
      expect(can("delete")).toBe(false);
    });

    it("HOMEROOM_TEACHER has view, enter, export", () => {
      const authStore = useAuthStore();
      authStore.token = "t";
      authStore.user = { id: 3, username: "teacher", roles: ["HOMEROOM_TEACHER"], activeRole: "HOMEROOM_TEACHER" };

      const { can } = useRBAC();

      expect(can("view")).toBe(true);
      expect(can("enter")).toBe(true);
      expect(can("export")).toBe(true);
      expect(can("create")).toBe(false);
      expect(can("import")).toBe(false);
      expect(can("review")).toBe(false);
      expect(can("publish")).toBe(false);
    });

    it("ACADEMIC_AFFAIRS has create/import/review/publish but not delete", () => {
      const authStore = useAuthStore();
      authStore.token = "t";
      authStore.user = { id: 4, username: "acad", roles: ["ACADEMIC_AFFAIRS"], activeRole: "ACADEMIC_AFFAIRS" };

      const { can } = useRBAC();

      expect(can("create")).toBe(true);
      expect(can("import")).toBe(true);
      expect(can("review")).toBe(true);
      expect(can("publish")).toBe(true);
      expect(can("delete")).toBe(false);
    });
  });

  describe("Server-provided permissions override", () => {
    it("uses server permissions when provided", () => {
      const authStore = useAuthStore();
      authStore.token = "t";
      authStore.user = {
        id: 5,
        username: "custom",
        roles: ["ADMIN"],
        activeRole: "ADMIN",
        permissions: ["view", "export"],
      };

      const { can, permissions } = useRBAC();

      expect(permissions.value).toEqual(["view", "export"]);
      expect(can("view")).toBe(true);
      expect(can("export")).toBe(true);
      expect(can("create")).toBe(false);
      expect(can("delete")).toBe(false);
    });
  });

  describe("canAny role check", () => {
    it("returns true if user role matches one in list", () => {
      const authStore = useAuthStore();
      authStore.token = "t";
      authStore.user = { id: 1, username: "admin", roles: ["ADMIN"], activeRole: "ADMIN" };

      const { canAny } = useRBAC();
      expect(canAny(["ADMIN", "ACADEMIC_AFFAIRS"])).toBe(true);
    });

    it("returns false if user role does not match", () => {
      const authStore = useAuthStore();
      authStore.token = "t";
      authStore.user = { id: 2, username: "student", roles: ["STUDENT"], activeRole: "STUDENT" };

      const { canAny } = useRBAC();
      expect(canAny(["ADMIN", "ACADEMIC_AFFAIRS"])).toBe(false);
    });

    it("returns false when not authenticated", () => {
      const { canAny } = useRBAC();
      expect(canAny(["ADMIN"])).toBe(false);
    });
  });

  describe("canAll and canAnyAction", () => {
    it("canAll returns true only when user has all specified permissions", () => {
      const authStore = useAuthStore();
      authStore.token = "t";
      authStore.user = { id: 1, username: "admin", roles: ["ADMIN"], activeRole: "ADMIN" };

      const { canAll } = useRBAC();
      expect(canAll(["view", "create", "delete"])).toBe(true);
    });

    it("canAll returns false when missing any permission", () => {
      const authStore = useAuthStore();
      authStore.token = "t";
      authStore.user = { id: 2, username: "teacher", roles: ["HOMEROOM_TEACHER"], activeRole: "HOMEROOM_TEACHER" };

      const { canAll } = useRBAC();
      expect(canAll(["view", "create"])).toBe(false);
    });

    it("canAnyAction returns true when at least one permission matches", () => {
      const authStore = useAuthStore();
      authStore.token = "t";
      authStore.user = { id: 2, username: "teacher", roles: ["HOMEROOM_TEACHER"], activeRole: "HOMEROOM_TEACHER" };

      const { canAnyAction } = useRBAC();
      expect(canAnyAction(["create", "export"])).toBe(true);
    });

    it("canAnyAction returns false when no permission matches", () => {
      const authStore = useAuthStore();
      authStore.token = "t";
      authStore.user = { id: 3, username: "student", roles: ["STUDENT"], activeRole: "STUDENT" };

      const { canAnyAction } = useRBAC();
      expect(canAnyAction(["create", "import", "delete"])).toBe(false);
    });
  });
});
