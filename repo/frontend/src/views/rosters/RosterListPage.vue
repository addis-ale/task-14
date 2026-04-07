<script setup lang="ts">
import { reactive, ref } from "vue";
import * as XLSX from "xlsx";
import DataTable from "@/components/DataTable/DataTable.vue";
import { api, unwrap } from "@/api";
import { useRBAC } from "@/composables/useRBAC";
import type { PageData } from "@/types/api";
import type { TableColumn } from "@/types/ui";

const { can } = useRBAC();

const filters = reactive({
  sessionId: "",
  status: "",
});

const columns: TableColumn[] = [
  { key: "studentNo", label: "学号 Student ID", sortable: true, maskPii: true },
  { key: "studentName", label: "姓名 Name", sortable: true },
  { key: "seatNo", label: "座位 Seat", sortable: true },
  { key: "status", label: "状态 Status", sortable: true },
];

const latestRows = ref<Record<string, unknown>[]>([]);

async function fetcher(
  params: Record<string, unknown>,
): Promise<PageData<Record<string, unknown>>> {
  if (!filters.sessionId) {
    return {
      items: [],
      pagination: { page: 1, size: 20, totalItems: 0, totalPages: 0 },
    };
  }
  return unwrap(
    api.get(`/sessions/${filters.sessionId}/candidates`, {
      params: { ...params, status: filters.status },
    }),
  );
}

function cacheRows(rows: Record<string, unknown>[]): void {
  latestRows.value = rows;
}

async function saveSeat(
  row: Record<string, unknown>,
  seatNo: string,
): Promise<void> {
  if (!can("update")) {
    return;
  }
  await unwrap(
    api.put(`/sessions/${filters.sessionId}/candidates/${row.studentId}/seat`, {
      seatNumber: Number(seatNo),
      roomId: row.roomId,
    }),
  );
}

function promptSeat(row: Record<string, unknown>): void {
  const next = String(
    window.prompt("输入新座位号", String(row.seatNo || "")) || row.seatNo || "",
  );
  void saveSeat(row, next);
}

function exportData(type: "csv" | "xlsx"): void {
  if (latestRows.value.length === 0) {
    return;
  }

  const workbook = XLSX.utils.book_new();
  const worksheet = XLSX.utils.json_to_sheet(latestRows.value);
  XLSX.utils.book_append_sheet(workbook, worksheet, "Roster");
  XLSX.writeFile(workbook, `roster-export.${type}`);
}
</script>

<template>
  <section class="page-grid">
    <header>
      <h2>名册管理</h2>
      <p>Roster table with inline seat editing and export</p>
    </header>

    <div class="actions card">
      <input
        v-model="filters.sessionId"
        placeholder="请输入场次ID Session ID"
      />
      <input v-model="filters.status" placeholder="状态过滤 Status" />
      <RouterLink v-if="can('import')" class="primary-link" to="/rosters/import"
        >导入 Import</RouterLink
      >
      <button v-if="can('export')" type="button" @click="exportData('csv')">导出CSV</button>
      <button v-if="can('export')" type="button" @click="exportData('xlsx')">导出XLSX</button>
    </div>

    <DataTable
      :columns="columns"
      :fetcher="fetcher"
      :filters="filters"
      @loaded="cacheRows"
    >
      <template #actions="{ row }">
        <button v-if="can('update')" type="button" @click.stop="promptSeat(row)">
          改座位 Edit Seat
        </button>
      </template>
    </DataTable>
  </section>
</template>

<style scoped>
.page-grid {
  display: grid;
  gap: 12px;
}

h2 {
  margin: 0;
}

header p {
  margin: 5px 0 0;
  color: var(--color-text-soft);
}

.actions {
  padding: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.actions input,
.actions button,
.primary-link {
  min-height: 36px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  padding: 0 10px;
}

.primary-link {
  text-decoration: none;
  background: var(--color-primary);
  color: white;
  border-color: var(--color-primary);
  display: inline-flex;
  align-items: center;
}
</style>
