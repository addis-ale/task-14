import { computed } from "vue";
import { useAuthStore } from "@/stores/auth";
import type { RoleName } from "@/types/ui";

export function useRBAC() {
  const authStore = useAuthStore();

  const role = computed(() => authStore.activeRole as RoleName | "");

  function canAny(roles: RoleName[]): boolean {
    if (!role.value) {
      return false;
    }
    return roles.includes(role.value as RoleName);
  }

  return {
    role,
    canAny,
  };
}
