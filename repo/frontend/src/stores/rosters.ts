import { ref } from "vue";
import { defineStore } from "pinia";
import { api, unwrap } from "@/api";
import type { PageData } from "@/types/api";

export interface RosterRow {
  studentId: number;
  studentNo: string;
  studentName: string;
  seatNo: string;
  status: string;
}

export const useRostersStore = defineStore("rosters", () => {
  const rows = ref<PageData<RosterRow> | null>(null);
  const loading = ref(false);

  async function fetchBySession(
    sessionId: number,
    page = 1,
    size = 20,
  ): Promise<void> {
    loading.value = true;
    try {
      rows.value = await unwrap(
        api.get<PageData<RosterRow>>(`/sessions/${sessionId}/candidates`, {
          params: { page, size },
        }),
      );
    } finally {
      loading.value = false;
    }
  }

  async function updateSeat(
    sessionId: number,
    studentId: number,
    seatNo: string,
  ): Promise<void> {
    await unwrap(
      api.put(`/sessions/${sessionId}/candidates/${studentId}/seat`, {
        seatNo,
      }),
    );
  }

  return {
    rows,
    loading,
    fetchBySession,
    updateSeat,
  };
});
