<script setup lang="ts">
import { reactive } from "vue";
import { useRouter } from "vue-router";
import DataTable from "@/components/DataTable/DataTable.vue";
import { api, unwrap } from "@/api";
import { useRBAC } from "@/composables/useRBAC";
import type { PageData } from "@/types/api";
import type { TableColumn } from "@/types/ui";

const { can } = useRBAC();

const router = useRouter();

const filters = reactive({
  termId: "",
  gradeId: "",
  subjectId: "",
  date: "",
});

const columns: TableColumn[] = [
  { key: "id", label: "ID", sortable: true, width: "90px" },
  { key: "termName", label: "学期 Term", sortable: true },
  { key: "gradeName", label: "年级 Grade", sortable: true },
  { key: "subjectName", label: "科目 Subject", sortable: true },
  { key: "date", label: "日期 Date", sortable: true },
  { key: "startTime", label: "开始 Start", sortable: true },
  { key: "endTime", label: "结束 End", sortable: true },
];

async function fetcher(
  params: Record<string, unknown>,
): Promise<PageData<Record<string, unknown>>> {
  return unwrap(api.get("/sessions", { params: { ...params, ...filters } }));
}

function toDetail(row: Record<string, unknown>): void {
  void router.push(`/scheduling/sessions/${row.id}`);
}
</script>

<template>
  <section class="page-grid">
    <header class="head-row">
      <div>
        <h2>考试场次列表</h2>
        <p>Exam session list with term/grade/subject/date filters</p>
      </div>
      <button v-if="can('create')" type="button" class="primary-btn" @click="router.push('/scheduling/sessions/new')">
        新建排考 Create Session
      </button>
    </header>

    <DataTable
      :columns="columns"
      :fetcher="fetcher"
      :filters="filters"
      search-placeholder="搜索科目/场次 Search sessions"
      @row-click="toDetail"
    >
      <template #filters>
        <div class="filters-row">
          <input v-model="filters.termId" placeholder="学期ID" />
          <input v-model="filters.gradeId" placeholder="年级ID" />
          <input v-model="filters.subjectId" placeholder="科目ID" />
          <input v-model="filters.date" type="date" />
        </div>
      </template>
      <template #actions="{ row }">
        <button type="button" @click.stop="toDetail(row)">详情 Detail</button>
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

.head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

header p {
  color: var(--color-text-soft);
  margin: 5px 0 0;
}

.primary-btn {
  min-height: 36px;
  border-radius: 10px;
  border: 1px solid var(--color-primary);
  background: var(--color-primary);
  color: white;
  padding: 0 14px;
  font: inherit;
  cursor: pointer;
}

.filters-row {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.filters-row input {
  min-height: 36px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  padding: 0 10px;
}
</style>
