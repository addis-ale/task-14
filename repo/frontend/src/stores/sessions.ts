import { ref } from "vue";
import { defineStore } from "pinia";
import { api, unwrap } from "@/api";
import type { PageData } from "@/types/api";

export interface ExamSessionItem {
  id: number;
  termId: number;
  gradeId: number;
  subjectId: number;
  examDate: string;
  startTime: string;
  endTime: string;
  status: string;
}

export const useSessionsStore = defineStore("sessions", () => {
  const loading = ref(false);
  const sessions = ref<PageData<ExamSessionItem> | null>(null);

  async function fetchSessions(params: Record<string, unknown>): Promise<void> {
    loading.value = true;
    try {
      sessions.value = await unwrap(
        api.get<PageData<ExamSessionItem>>("/sessions", { params }),
      );
    } finally {
      loading.value = false;
    }
  }

  return {
    loading,
    sessions,
    fetchSessions,
  };
});
