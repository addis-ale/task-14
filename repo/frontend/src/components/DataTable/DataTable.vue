<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { maskStudentId } from "@/utils/pii";
import type { PageData } from "@/types/api";
import type { TableColumn } from "@/types/ui";

type RowData = Record<string, unknown>;

const props = withDefaults(
  defineProps<{
    columns: TableColumn[];
    fetcher: (params: Record<string, unknown>) => Promise<PageData<RowData>>;
    pageSize?: number;
    searchPlaceholder?: string;
    filters?: Record<string, unknown>;
  }>(),
  {
    pageSize: 20,
    searchPlaceholder: "搜索 Search...",
  },
);

const emit = defineEmits<{
  loaded: [rows: RowData[]];
  rowClick: [row: RowData];
}>();

const loading = ref(false);
const fetchError = ref("");
const search = ref("");
const sort = ref("");
const order = ref<"asc" | "desc">("asc");
const page = ref(1);
const size = ref(props.pageSize);
const showRawPii = ref(false);

const tableData = ref<PageData<RowData>>({
  items: [],
  pagination: {
    page: 1,
    size: props.pageSize,
    totalItems: 0,
    totalPages: 0,
  },
});

const totalPages = computed(() =>
  Math.max(1, tableData.value.pagination.totalPages || 1),
);

async function loadTable(): Promise<void> {
  loading.value = true;
  fetchError.value = "";
  try {
    const params = {
      page: page.value,
      size: size.value,
      sort: sort.value,
      order: order.value,
      search: search.value,
      ...(props.filters || {}),
    };
    const result = await props.fetcher(params);
    tableData.value = result;
    emit("loaded", result.items);
  } catch (err) {
    const msg = (err as { response?: { data?: { message?: string } }; message?: string })
      ?.response?.data?.message || (err as Error)?.message || "数据加载失败 Failed to load data";
    fetchError.value = msg;
  } finally {
    loading.value = false;
  }
}

function toggleSort(columnKey: string): void {
  if (sort.value !== columnKey) {
    sort.value = columnKey;
    order.value = "asc";
  } else {
    order.value = order.value === "asc" ? "desc" : "asc";
  }
  void loadTable();
}

function goToPage(targetPage: number): void {
  page.value = Math.min(Math.max(1, targetPage), totalPages.value);
  void loadTable();
}

function onSearchSubmit(): void {
  page.value = 1;
  void loadTable();
}

function renderCell(row: RowData, column: TableColumn): string {
  const value = row[column.key];
  if (column.maskPii) {
    return maskStudentId(value as string | number, showRawPii.value);
  }
  if (value === null || value === undefined || value === "") {
    return "-";
  }
  return String(value);
}

watch(
  () => props.filters,
  () => {
    page.value = 1;
    void loadTable();
  },
  { deep: true },
);

onMounted(() => {
  void loadTable();
});
</script>

<template>
  <section class="table-shell" aria-label="Data table">
    <header class="table-toolbar">
      <form class="search-bar" @submit.prevent="onSearchSubmit">
        <input
          v-model="search"
          type="search"
          :placeholder="searchPlaceholder"
        />
        <button type="submit">查询</button>
      </form>

      <div class="toolbar-extra">
        <slot name="filters" />
        <label class="mask-toggle">
          <input v-model="showRawPii" type="checkbox" />
          显示完整学号 Show full ID
        </label>
      </div>
    </header>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th
              v-for="col in columns"
              :key="col.key"
              :style="{ width: col.width || 'auto' }"
            >
              <button
                v-if="col.sortable"
                class="sort-btn"
                type="button"
                @click="toggleSort(col.key)"
              >
                {{ col.label }}
                <small v-if="sort === col.key">{{
                  order === "asc" ? "ASC" : "DESC"
                }}</small>
              </button>
              <span v-else>{{ col.label }}</span>
            </th>
            <th>
              操作
              <small>Actions</small>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-if="loading"
            v-for="index in 6"
            :key="`skeleton-${index}`"
            class="skeleton-row"
          >
            <td v-for="col in columns" :key="`${index}-${col.key}`">
              <span class="skeleton" />
            </td>
            <td><span class="skeleton" /></td>
          </tr>
          <tr
            v-for="row in tableData.items"
            :key="JSON.stringify(row)"
            @click="emit('rowClick', row)"
            tabindex="0"
          >
            <td v-for="col in columns" :key="col.key">
              <slot :name="`cell-${col.key}`" :row="row">
                {{ renderCell(row, col) }}
              </slot>
            </td>
            <td>
              <details class="action-menu" @click.stop>
                <summary>操作</summary>
                <div class="action-panel">
                  <slot name="actions" :row="row">
                    <button type="button">查看</button>
                  </slot>
                </div>
              </details>
            </td>
          </tr>
          <tr v-if="!loading && fetchError">
            <td :colspan="columns.length + 1" class="error-state">
              <div class="error-content">
                <span>加载失败 Load failed: {{ fetchError }}</span>
                <button type="button" class="retry-btn" @click="loadTable">重试 Retry</button>
              </div>
            </td>
          </tr>
          <tr v-else-if="!loading && !fetchError && tableData.items.length === 0">
            <td :colspan="columns.length + 1" class="empty">
              暂无数据 No data
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <footer class="table-pagination">
      <button type="button" :disabled="page <= 1" @click="goToPage(page - 1)">
        上一页
      </button>
      <span>第 {{ page }} / {{ totalPages }} 页</span>
      <button
        type="button"
        :disabled="page >= totalPages"
        @click="goToPage(page + 1)"
      >
        下一页
      </button>
    </footer>
  </section>
</template>

<style scoped>
.table-shell {
  display: grid;
  gap: 12px;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
}

.search-bar {
  display: flex;
  gap: 8px;
}

.search-bar input {
  min-width: 220px;
  min-height: 36px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
}

.search-bar button,
.table-pagination button,
tbody button {
  min-height: 36px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  background: #ffffff;
  padding: 0 12px;
  cursor: pointer;
}

.toolbar-extra {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mask-toggle {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  font-size: 0.86rem;
}

.table-wrap {
  overflow: auto;
  border: 1px solid var(--color-border);
  border-radius: 12px;
}

table {
  width: 100%;
  border-collapse: collapse;
  min-width: 780px;
}

thead {
  background: var(--color-surface-alt);
}

th,
td {
  text-align: left;
  border-bottom: 1px solid var(--color-border);
  padding: 10px 12px;
  vertical-align: top;
}

tbody tr {
  transition: background-color 0.2s ease;
}

tbody tr:hover,
tbody tr:focus-within {
  background: #f4fbff;
}

.action-menu {
  position: relative;
}

.action-menu summary {
  cursor: pointer;
  list-style: none;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  min-height: 32px;
  display: inline-flex;
  align-items: center;
  padding: 0 10px;
}

.action-menu[open] .action-panel {
  display: flex;
}

.action-panel {
  display: none;
  gap: 6px;
  flex-wrap: wrap;
  padding-top: 6px;
}

.sort-btn {
  border: none;
  padding: 0;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.sort-btn small {
  margin-left: 4px;
  color: var(--color-text-soft);
}

.skeleton {
  display: block;
  height: 15px;
  border-radius: 8px;
  background: linear-gradient(90deg, #f2f7fa 0%, #e3eef4 50%, #f2f7fa 100%);
  animation: pulse 1.2s infinite ease-in-out;
}

.empty {
  text-align: center;
  color: var(--color-text-soft);
}

.error-state {
  text-align: center;
}

.error-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 10px;
  color: var(--color-danger);
}

.retry-btn {
  background: #fdeeed;
  border-color: #f7c6c2;
  color: #9e3a35;
  min-height: 32px;
  padding: 0 12px;
  cursor: pointer;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  align-items: center;
}

@keyframes pulse {
  0% {
    opacity: 0.55;
  }
  50% {
    opacity: 1;
  }
  100% {
    opacity: 0.55;
  }
}
</style>
