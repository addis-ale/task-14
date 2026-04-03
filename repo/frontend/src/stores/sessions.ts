import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { api, unwrap } from "@/api";
import { handleApiError, showSuccess } from "@/utils/toast";
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
  termName?: string;
  gradeName?: string;
  subjectName?: string;
  roomCount?: number;
  candidateCount?: number;
}

export interface SessionFilters {
  termId?: string;
  gradeId?: string;
  subjectId?: string;
  date?: string;
  status?: string;
}

export interface CreateSessionPayload {
  termId: number;
  gradeId: number;
  subjectId: number;
  examDate: string;
  startTime: string;
  endTime: string;
}

export const useSessionsStore = defineStore("sessions", () => {
  const loading = ref(false);
  const error = ref<string | null>(null);
  const sessions = ref<PageData<ExamSessionItem> | null>(null);
  const currentSession = ref<ExamSessionItem | null>(null);
  const page = ref(1);
  const pageSize = ref(20);

  const items = computed(() => sessions.value?.items || []);
  const totalItems = computed(() => sessions.value?.pagination?.totalItems || 0);
  const totalPages = computed(() => sessions.value?.pagination?.totalPages || 0);
  const isEmpty = computed(() => !loading.value && items.value.length === 0);

  async function fetchSessions(
    params: Record<string, unknown> = {},
    filters: SessionFilters = {},
  ): Promise<void> {
    loading.value = true;
    error.value = null;
    try {
      const queryParams = {
        page: page.value,
        size: pageSize.value,
        ...params,
        ...Object.fromEntries(
          Object.entries(filters).filter(([, v]) => v !== undefined && v !== ""),
        ),
      };
      sessions.value = await unwrap(
        api.get<PageData<ExamSessionItem>>("/sessions", { params: queryParams }),
      );
    } catch (err) {
      error.value = "加载场次失败 Failed to load sessions";
      handleApiError(err);
    } finally {
      loading.value = false;
    }
  }

  async function fetchSessionById(id: number): Promise<ExamSessionItem | null> {
    loading.value = true;
    error.value = null;
    try {
      currentSession.value = await unwrap(
        api.get<ExamSessionItem>(`/sessions/${id}`),
      );
      return currentSession.value;
    } catch (err) {
      error.value = "加载场次详情失败 Failed to load session detail";
      handleApiError(err);
      return null;
    } finally {
      loading.value = false;
    }
  }

  async function createSession(payload: CreateSessionPayload): Promise<ExamSessionItem | null> {
    loading.value = true;
    error.value = null;
    try {
      const created = await unwrap(api.post<ExamSessionItem>("/sessions", payload));
      showSuccess("场次创建成功 Session created");
      return created;
    } catch (err) {
      error.value = "创建场次失败 Failed to create session";
      handleApiError(err);
      return null;
    } finally {
      loading.value = false;
    }
  }

  async function updateSession(
    id: number,
    payload: Partial<CreateSessionPayload>,
  ): Promise<boolean> {
    loading.value = true;
    error.value = null;
    try {
      await unwrap(api.put(`/sessions/${id}`, payload));
      showSuccess("场次更新成功 Session updated");
      return true;
    } catch (err) {
      error.value = "更新场次失败 Failed to update session";
      handleApiError(err);
      return false;
    } finally {
      loading.value = false;
    }
  }

  async function deleteSession(id: number): Promise<boolean> {
    loading.value = true;
    error.value = null;
    try {
      await unwrap(api.delete(`/sessions/${id}`));
      showSuccess("场次删除成功 Session deleted");
      return true;
    } catch (err) {
      error.value = "删除场次失败 Failed to delete session";
      handleApiError(err);
      return false;
    } finally {
      loading.value = false;
    }
  }

  async function publishSession(id: number): Promise<boolean> {
    error.value = null;
    try {
      await unwrap(api.post(`/sessions/${id}/publish`));
      showSuccess("场次发布成功 Session published");
      return true;
    } catch (err) {
      error.value = "发布失败 Failed to publish session";
      handleApiError(err);
      return false;
    }
  }

  function setPage(p: number): void {
    page.value = p;
  }

  function clearError(): void {
    error.value = null;
  }

  return {
    loading,
    error,
    sessions,
    currentSession,
    page,
    pageSize,
    items,
    totalItems,
    totalPages,
    isEmpty,
    fetchSessions,
    fetchSessionById,
    createSession,
    updateSession,
    deleteSession,
    publishSession,
    setPage,
    clearError,
  };
});
