<script setup lang="ts">
import { reactive, ref } from "vue";
import DataTable from "@/components/DataTable/DataTable.vue";
import { api, unwrap } from "@/api";
import { handleApiError, showSuccess } from "@/utils/toast";
import { formatDateTime } from "@/utils/date";
import { useI18n } from "@/i18n";
import type { PageData } from "@/types/api";
import type { TableColumn } from "@/types/ui";

const { t } = useI18n();
const filters = reactive({ jobType: "", status: "" });
const tableKey = ref(0);

const jobTypeOptions = [
  "NOTIFICATION_SEND",
  "ROSTER_IMPORT",
  "EXAM_PUBLISH",
  "REPORT_GENERATE",
  "CLEANUP",
];

const statusOptions = [
  { value: "PENDING", label: t("jobs.pending") },
  { value: "RUNNING", label: t("jobs.running") },
  { value: "COMPLETED", label: t("jobs.completed") },
  { value: "FAILED", label: t("jobs.failed") },
  { value: "CANCELLED", label: t("jobs.cancelled") },
];

const columns: TableColumn[] = [
  { key: "jobType", label: "任务类型 Type", sortable: true },
  { key: "dedupKey", label: "去重键 Dedup Key", sortable: true },
  { key: "status", label: "状态 Status", sortable: true },
  { key: "attempts", label: "尝试次数 Attempts", sortable: true },
  { key: "errorMessage", label: "错误信息 Error", sortable: false },
  { key: "createdAt", label: "创建时间 Created", sortable: true },
  { key: "completedAt", label: "完成时间 Completed", sortable: true },
];

const detail = ref<Record<string, unknown> | null>(null);
const detailVisible = ref(false);

function statusColor(status: unknown): string {
  switch (status) {
    case "COMPLETED": return "badge-green";
    case "FAILED": return "badge-red";
    case "RUNNING": return "badge-yellow";
    case "PENDING": return "badge-gray";
    case "CANCELLED": return "badge-gray";
    default: return "badge-gray";
  }
}

async function fetcher(
  params: Record<string, unknown>,
): Promise<PageData<Record<string, unknown>>> {
  return unwrap(api.get("/jobs", { params: { ...params, ...filters } }));
}

async function openDetail(row: Record<string, unknown>): Promise<void> {
  try {
    detail.value = await unwrap(api.get(`/jobs/${row.id}`));
    detailVisible.value = true;
  } catch (err) {
    handleApiError(err);
  }
}

async function retry(row: Record<string, unknown>): Promise<void> {
  try {
    await unwrap(api.post(`/jobs/${row.id}/retry`));
    showSuccess(t("common.success"));
    tableKey.value++;
  } catch (err) {
    handleApiError(err);
  }
}
</script>

<template>
  <section class="page-grid">
    <nav class="breadcrumb" aria-label="Breadcrumb">
      <span>{{ t("nav.dashboard") }}</span> / <span>{{ t("nav.jobs") }}</span>
    </nav>

    <header>
      <h2>{{ t("jobs.title") }}</h2>
      <p>{{ t("jobs.subtitle") }}</p>
    </header>

    <DataTable :key="tableKey" :columns="columns" :fetcher="fetcher" :filters="filters">
      <template #filters>
        <div class="filters-row">
          <select v-model="filters.jobType" aria-label="Filter by job type">
            <option value="">{{ t("jobs.jobType") }}</option>
            <option v-for="jt in jobTypeOptions" :key="jt" :value="jt">{{ jt }}</option>
          </select>
          <select v-model="filters.status" aria-label="Filter by status">
            <option value="">{{ t("common.status") }}</option>
            <option v-for="s in statusOptions" :key="s.value" :value="s.value">{{ s.label }}</option>
          </select>
        </div>
      </template>
      <template #cell-status="{ row }">
        <span class="status-badge" :class="statusColor(row.status)">
          {{ row.status }}
        </span>
      </template>
      <template #cell-createdAt="{ row }">
        {{ formatDateTime(row.createdAt as string) }}
      </template>
      <template #cell-completedAt="{ row }">
        {{ formatDateTime(row.completedAt as string) }}
      </template>
      <template #cell-errorMessage="{ row }">
        <span class="error-text" :title="String(row.errorMessage || '')">
          {{ row.errorMessage ? String(row.errorMessage).substring(0, 60) : '-' }}
        </span>
      </template>
      <template #actions="{ row }">
        <div class="row-actions">
          <button type="button" @click.stop="openDetail(row)">{{ t("jobs.detail") }}</button>
          <button
            v-if="row.status === 'FAILED'"
            type="button"
            class="retry-btn"
            @click.stop="retry(row)"
          >{{ t("common.retry") }}</button>
        </div>
      </template>
    </DataTable>

    <!-- Job Detail Modal -->
    <div v-if="detailVisible && detail" class="modal-backdrop" role="dialog" aria-modal="true" @click.self="detailVisible = false">
      <div class="modal card">
        <header class="modal-header">
          <h3>{{ t("jobs.detail") }} #{{ detail.id }}</h3>
          <button type="button" class="outline-btn" @click="detailVisible = false">{{ t("common.close") }}</button>
        </header>
        <div class="detail-grid">
          <div class="detail-item">
            <strong>{{ t("jobs.jobType") }}</strong>
            <span>{{ detail.jobType }}</span>
          </div>
          <div class="detail-item">
            <strong>{{ t("jobs.dedupKey") }}</strong>
            <span>{{ detail.dedupKey || '-' }}</span>
          </div>
          <div class="detail-item">
            <strong>{{ t("jobs.status") }}</strong>
            <span class="status-badge" :class="statusColor(detail.status)">{{ detail.status }}</span>
          </div>
          <div class="detail-item">
            <strong>{{ t("jobs.attempts") }}</strong>
            <span>{{ detail.attempts }}</span>
          </div>
          <div class="detail-item" v-if="detail.errorMessage">
            <strong>{{ t("jobs.errorMessage") }}</strong>
            <span class="error-text">{{ detail.errorMessage }}</span>
          </div>
        </div>
        <div class="payload-section">
          <strong>{{ t("jobs.payload") }} (payload_json)</strong>
          <pre>{{ JSON.stringify(detail.payloadJson || detail.payload || detail, null, 2) }}</pre>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.page-grid {
  display: grid;
  gap: 12px;
}

.breadcrumb {
  font-size: 0.85rem;
  color: var(--color-text-soft);
}

h2, h3 {
  margin: 0;
}

p {
  margin: 5px 0 0;
  color: var(--color-text-soft);
}

.filters-row, .row-actions {
  display: flex;
  gap: 6px;
}

select, input, button {
  min-height: 36px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  padding: 0 10px;
  font: inherit;
}

.status-badge {
  font-size: 0.8rem;
  padding: 2px 10px;
  border-radius: 999px;
  font-weight: 500;
  display: inline-block;
}

.badge-green { background: #e8f8ef; color: #2d8f57; }
.badge-red { background: #fdeeed; color: #9e3a35; }
.badge-yellow { background: #fef9e7; color: #b7791f; }
.badge-gray { background: #f0f2f4; color: #6b7280; }

.retry-btn {
  background: #fdeeed;
  border-color: #f7c6c2;
  color: #9e3a35;
}

.error-text {
  color: var(--color-danger);
  font-size: 0.85rem;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(25, 40, 50, 0.32);
  display: grid;
  place-items: center;
  z-index: 20;
}

.modal {
  width: min(640px, calc(100vw - 28px));
  padding: 18px;
  max-height: 90vh;
  overflow: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 12px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item strong {
  font-size: 0.82rem;
  color: var(--color-text-soft);
}

.payload-section {
  margin-top: 8px;
}

.payload-section strong {
  display: block;
  margin-bottom: 6px;
  font-size: 0.85rem;
  color: var(--color-text-soft);
}

pre {
  margin: 0;
  overflow: auto;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  background: #f6fafc;
  font-size: 0.82rem;
  max-height: 300px;
}

.outline-btn {
  background: white;
  cursor: pointer;
}
</style>
