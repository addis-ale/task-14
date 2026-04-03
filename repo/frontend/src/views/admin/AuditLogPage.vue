<script setup lang="ts">
import { reactive, ref } from "vue";
import DataTable from "@/components/DataTable/DataTable.vue";
import { api, unwrap } from "@/api";
import { formatDateTime } from "@/utils/date";
import { handleApiError } from "@/utils/toast";
import { useI18n } from "@/i18n";
import { useRBAC } from "@/composables/useRBAC";
import type { PageData } from "@/types/api";
import type { TableColumn } from "@/types/ui";

const { can } = useRBAC();

const { t } = useI18n();
const filters = reactive({ actorId: "", entityType: "", actionType: "", from: "", to: "" });
const expandedRowId = ref<unknown>(null);
const expandedRow = ref<Record<string, unknown> | null>(null);

const resourceTypes = [
  "ExamSession", "Roster", "Notification", "User", "ComplianceReview",
  "ProctorAssign", "RoomAssignment", "Campus", "Room",
];

const actionTypes = [
  "CREATE", "UPDATE", "DELETE", "LOGIN", "LOGOUT", "APPROVE",
  "REJECT", "PUBLISH", "IMPORT", "EXPORT", "VIEW_PII",
];

const columns: TableColumn[] = [
  { key: "actorId", label: "操作者 User", sortable: true },
  { key: "actionType", label: "操作 Action", sortable: true },
  { key: "entityType", label: "资源类型 Resource", sortable: true },
  { key: "entityId", label: "资源ID", sortable: true },
  { key: "ipAddress", label: "IP地址 IP Address", sortable: false },
  { key: "createdAt", label: "时间 Timestamp", sortable: true },
];

async function fetcher(
  params: Record<string, unknown>,
): Promise<PageData<Record<string, unknown>>> {
  return unwrap(api.get("/audit-logs", { params: { ...params, ...filters } }));
}

function toggleRow(row: Record<string, unknown>) {
  if (expandedRowId.value === row.id) {
    expandedRowId.value = null;
    expandedRow.value = null;
  } else {
    expandedRowId.value = row.id;
    expandedRow.value = row;
  }
}

function formatJson(data: unknown): string {
  if (!data) return "{}";
  if (typeof data === "string") {
    try {
      return JSON.stringify(JSON.parse(data), null, 2);
    } catch {
      return data;
    }
  }
  return JSON.stringify(data, null, 2);
}

async function exportCsv(): Promise<void> {
  try {
    const params: Record<string, string> = {};
    if (filters.actorId) params.actorId = filters.actorId;
    if (filters.entityType) params.entityType = filters.entityType;
    if (filters.actionType) params.actionType = filters.actionType;
    if (filters.from) params.from = filters.from;
    if (filters.to) params.to = filters.to;

    const response = await api.get("/audit-logs/export", {
      params,
      responseType: "blob",
    });

    const blob = new Blob([response.data], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `audit-logs-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  } catch (err) {
    handleApiError(err);
  }
}
</script>

<template>
  <section class="page-grid">
    <nav class="breadcrumb" aria-label="Breadcrumb">
      <span>{{ t("nav.dashboard") }}</span> / <span>{{ t("nav.auditLogs") }}</span>
    </nav>

    <header class="head-row">
      <div>
        <h2>{{ t("audit.title") }}</h2>
        <p>{{ t("audit.subtitle") }}</p>
      </div>
      <button v-if="can('export')" type="button" class="outline-btn" @click="exportCsv">
        {{ t("audit.exportCsv") }}
      </button>
    </header>

    <DataTable
      :columns="columns"
      :fetcher="fetcher"
      :filters="filters"
      @row-click="toggleRow"
    >
      <template #filters>
        <div class="filters-row">
          <input v-model="filters.actorId" :placeholder="t('audit.user') + ' ID'" />
          <select v-model="filters.entityType" :aria-label="t('audit.filterByResource')">
            <option value="">{{ t("audit.filterByResource") }}</option>
            <option v-for="rt in resourceTypes" :key="rt" :value="rt">{{ rt }}</option>
          </select>
          <select v-model="filters.actionType" :aria-label="t('audit.filterByAction')">
            <option value="">{{ t("audit.filterByAction") }}</option>
            <option v-for="at in actionTypes" :key="at" :value="at">{{ at }}</option>
          </select>
          <input v-model="filters.from" type="date" :aria-label="t('audit.dateRange') + ' from'" />
          <input v-model="filters.to" type="date" :aria-label="t('audit.dateRange') + ' to'" />
        </div>
      </template>
      <template #cell-createdAt="{ row }">
        {{ formatDateTime(row.createdAt as string) }}
      </template>
      <template #actions="{ row }">
        <button type="button" @click.stop="toggleRow(row)">
          {{ expandedRowId === row.id ? '收起 Collapse' : '展开 Expand' }}
        </button>
      </template>
    </DataTable>

    <!-- Expanded detail panel -->
    <div v-if="expandedRow !== null" class="detail-panel card">
      <h4>{{ t("audit.details") }} #{{ expandedRowId }}</h4>
      <pre>{{ formatJson(expandedRow.detailsJson || expandedRow) }}</pre>
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

.head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

h2, h4 {
  margin: 0;
}

p {
  margin: 5px 0 0;
  color: var(--color-text-soft);
}

.filters-row {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

input, select, button {
  min-height: 36px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  padding: 0 10px;
  font: inherit;
}

.outline-btn {
  background: white;
  cursor: pointer;
}

.detail-panel {
  padding: 14px;
}

.detail-panel h4 {
  margin-bottom: 8px;
  font-size: 0.9rem;
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
</style>
