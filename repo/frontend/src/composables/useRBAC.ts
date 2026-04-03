import { computed } from "vue";
import { useAuthStore } from "@/stores/auth";
import type { RoleName } from "@/types/ui";
import type { ActionPermission } from "@/types/auth";

/**
 * Default permission matrix per role.
 * Used as fallback when the server does not provide explicit permissions.
 */
const ROLE_PERMISSIONS: Record<string, ActionPermission[]> = {
  ADMIN: ["view", "enter", "import", "review", "publish", "create", "update", "delete", "export", "assign"],
  ACADEMIC_AFFAIRS: ["view", "enter", "import", "review", "publish", "create", "update", "export", "assign"],
  HOMEROOM_TEACHER: ["view", "enter", "export"],
  SUBJECT_TEACHER: ["view", "enter", "export"],
  STUDENT: ["view"],
};

export function useRBAC() {
  const authStore = useAuthStore();

  const role = computed(() => authStore.activeRole as RoleName | "");

  const permissions = computed<ActionPermission[]>(() => {
    // Prefer server-provided permissions; fall back to role-based defaults
    if (authStore.user?.permissions && authStore.user.permissions.length > 0) {
      return authStore.user.permissions;
    }
    return ROLE_PERMISSIONS[role.value] || [];
  });

  /** Check if the current user has any of the specified roles */
  function canAny(roles: RoleName[]): boolean {
    if (!role.value) {
      return false;
    }
    return roles.includes(role.value as RoleName);
  }

  /** Check if the current user has a specific action permission */
  function can(action: ActionPermission): boolean {
    return permissions.value.includes(action);
  }

  /** Check if the current user has ALL of the specified action permissions */
  function canAll(actions: ActionPermission[]): boolean {
    return actions.every((action) => permissions.value.includes(action));
  }

  /** Check if the current user has ANY of the specified action permissions */
  function canAnyAction(actions: ActionPermission[]): boolean {
    return actions.some((action) => permissions.value.includes(action));
  }

  return {
    role,
    permissions,
    canAny,
    can,
    canAll,
    canAnyAction,
  };
}
