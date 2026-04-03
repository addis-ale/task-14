import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { api, unwrap } from "@/api";
import { handleApiError, showSuccess } from "@/utils/toast";
import type { PageData } from "@/types/api";

export interface RosterRow {
  studentId: number;
  studentNo: string;
  studentName: string;
  seatNo: string;
  status: string;
  className?: string;
  gradeName?: string;
}

export interface RosterFilters {
  sessionId?: string;
  status?: string;
  search?: string;
}

export const useRostersStore = defineStore("rosters", () => {
  const rows = ref<PageData<RosterRow> | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const page = ref(1);
  const pageSize = ref(20);

  const items = computed(() => rows.value?.items || []);
  const totalItems = computed(() => rows.value?.pagination?.totalItems || 0);
  const totalPages = computed(() => rows.value?.pagination?.totalPages || 0);
  const isEmpty = computed(() => !loading.value && items.value.length === 0);

  async function fetchBySession(
    sessionId: number,
    pageNum = 1,
    size = 20,
    filters: Partial<RosterFilters> = {},
  ): Promise<void> {
    loading.value = true;
    error.value = null;
    page.value = pageNum;
    try {
      const params: Record<string, unknown> = { page: pageNum, size };
      if (filters.status) params.status = filters.status;
      if (filters.search) params.search = filters.search;
      rows.value = await unwrap(
        api.get<PageData<RosterRow>>(`/sessions/${sessionId}/candidates`, {
          params,
        }),
      );
    } catch (err) {
      error.value = "加载名册失败 Failed to load roster";
      handleApiError(err);
    } finally {
      loading.value = false;
    }
  }

  async function updateSeat(
    sessionId: number,
    studentId: number,
    seatNo: string,
  ): Promise<boolean> {
    error.value = null;
    try {
      await unwrap(
        api.put(`/sessions/${sessionId}/candidates/${studentId}/seat`, {
          seatNo,
        }),
      );
      showSuccess("座位更新成功 Seat updated");
      return true;
    } catch (err) {
      error.value = "更新座位失败 Failed to update seat";
      handleApiError(err);
      return false;
    }
  }

  async function removeCandidate(
    sessionId: number,
    studentId: number,
  ): Promise<boolean> {
    error.value = null;
    try {
      await unwrap(
        api.delete(`/sessions/${sessionId}/candidates/${studentId}`),
      );
      showSuccess("考生已移除 Candidate removed");
      return true;
    } catch (err) {
      error.value = "移除考生失败 Failed to remove candidate";
      handleApiError(err);
      return false;
    }
  }

  async function importRoster(
    sessionId: number,
    rosterRows: Array<Record<string, string>>,
  ): Promise<boolean> {
    loading.value = true;
    error.value = null;
    try {
      await unwrap(
        api.post("/imports/rosters/commit", {
          entityType: "SESSION_CANDIDATE",
          sessionId,
          rows: rosterRows,
        }),
      );
      showSuccess(`导入成功 ${rosterRows.length} rows imported`);
      return true;
    } catch (err) {
      error.value = "导入失败 Import failed";
      handleApiError(err);
      return false;
    } finally {
      loading.value = false;
    }
  }

  function clearError(): void {
    error.value = null;
  }

  function setPage(p: number): void {
    page.value = p;
  }

  return {
    rows,
    loading,
    error,
    page,
    pageSize,
    items,
    totalItems,
    totalPages,
    isEmpty,
    fetchBySession,
    updateSeat,
    removeCandidate,
    importRoster,
    clearError,
    setPage,
  };
});
