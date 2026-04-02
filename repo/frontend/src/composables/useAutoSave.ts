import { onBeforeUnmount, onMounted, ref, watch, type Ref } from "vue";
import { api, unwrap } from "@/api";

export interface AutoSaveOptions<T extends Record<string, unknown>> {
  formKey: string;
  data: Ref<T>;
  enabled?: Ref<boolean>;
  intervalMs?: number;
}

export function useAutoSave<T extends Record<string, unknown>>(
  options: AutoSaveOptions<T>,
) {
  const saving = ref(false);
  const lastSavedAt = ref<string>("");
  const error = ref("");
  const hasDraft = ref(false);
  let timer: number | undefined;

  async function saveDraft(): Promise<void> {
    if (options.enabled && !options.enabled.value) {
      return;
    }

    saving.value = true;
    error.value = "";
    try {
      await unwrap(api.put(`/drafts/${options.formKey}`, options.data.value));
      hasDraft.value = true;
      lastSavedAt.value = new Date().toISOString();
    } catch (err) {
      error.value = (err as Error).message;
    } finally {
      saving.value = false;
    }
  }

  async function loadDraft(): Promise<T | null> {
    try {
      const data = await unwrap(api.get<T>(`/drafts/${options.formKey}`));
      hasDraft.value = true;
      return data;
    } catch {
      return null;
    }
  }

  async function deleteDraft(): Promise<void> {
    try {
      await unwrap(api.delete(`/drafts/${options.formKey}`));
      hasDraft.value = false;
    } catch {
      // no-op
    }
  }

  onMounted(() => {
    const interval = options.intervalMs || 30_000;
    timer = window.setInterval(() => {
      void saveDraft();
    }, interval);
  });

  onBeforeUnmount(() => {
    if (timer) {
      window.clearInterval(timer);
    }
  });

  watch(
    () => options.data.value,
    () => {
      error.value = "";
    },
    { deep: true },
  );

  return {
    saving,
    lastSavedAt,
    error,
    hasDraft,
    saveDraft,
    loadDraft,
    deleteDraft,
  };
}
