<script setup lang="ts">
import { computed, ref } from "vue";
import * as XLSX from "xlsx";

interface PreviewRow {
  index: number;
  status: "valid" | "invalid" | "duplicate";
  reason: string;
  selected: boolean;
  values: Record<string, string>;
}

const props = defineProps<{
  requiredColumns: string[];
}>();

const emit = defineEmits<{
  commit: [rows: PreviewRow[]];
  cancel: [];
}>();

const previewRows = ref<PreviewRow[]>([]);
const columns = ref<string[]>([]);
const loading = ref(false);

const selectedValidRows = computed(() =>
  previewRows.value.filter((row) => row.status === "valid" && row.selected),
);

function onFileInput(event: Event): void {
  const files = (event.target as HTMLInputElement).files;
  if (!files || files.length === 0) {
    return;
  }
  void parseFile(files[0]);
}

async function parseFile(file: File): Promise<void> {
  loading.value = true;
  try {
    const buffer = await file.arrayBuffer();
    const workbook = XLSX.read(buffer);
    const firstSheet = workbook.Sheets[workbook.SheetNames[0]];
    const json = XLSX.utils.sheet_to_json<Record<string, unknown>>(firstSheet, {
      defval: "",
    });

    columns.value = Object.keys(json[0] || {});
    const seen = new Set<string>();
    previewRows.value = json.map((row, index) => {
      const normalized = Object.fromEntries(
        Object.entries(row).map(([k, v]) => [k, String(v ?? "").trim()]),
      );
      const serialized = JSON.stringify(normalized);

      let status: PreviewRow["status"] = "valid";
      let reason = "OK";

      const hasMissing = props.requiredColumns.some((col) => !normalized[col]);
      if (hasMissing) {
        status = "invalid";
        reason = "Missing required fields";
      } else if (seen.has(serialized)) {
        status = "duplicate";
        reason = "Duplicate row";
      }
      seen.add(serialized);

      return {
        index: index + 1,
        status,
        reason,
        selected: status === "valid",
        values: normalized,
      };
    });
  } finally {
    loading.value = false;
  }
}

function commit(): void {
  emit("commit", selectedValidRows.value);
}
</script>

<template>
  <section class="import-wizard">
    <header>
      <h3>批量导入向导</h3>
      <p>Bulk import with preview and row-level validation.</p>
    </header>

    <label class="dropzone card">
      <input type="file" accept=".csv,.xlsx" @change="onFileInput" />
      <strong>拖拽或点击上传 CSV/XLSX</strong>
      <small>Drag and drop supported</small>
    </label>

    <div class="progress" v-if="loading">
      <span />
      <small>解析中 Parsing...</small>
    </div>

    <div class="preview card" v-if="previewRows.length > 0">
      <div class="preview-head">
        <strong>预览结果 Preview</strong>
        <small
          >有效 {{ selectedValidRows.length }} / 总计
          {{ previewRows.length }}</small
        >
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>选择</th>
              <th>行号</th>
              <th>状态</th>
              <th>说明</th>
              <th v-for="column in columns" :key="column">{{ column }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in previewRows" :key="row.index">
              <td>
                <input
                  type="checkbox"
                  v-model="row.selected"
                  :disabled="row.status !== 'valid'"
                />
              </td>
              <td>{{ row.index }}</td>
              <td>
                <span class="status" :class="row.status">{{ row.status }}</span>
              </td>
              <td>{{ row.reason }}</td>
              <td v-for="column in columns" :key="`${row.index}-${column}`">
                {{ row.values[column] || "-" }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <footer class="actions">
        <button class="primary" type="button" @click="commit">
          提交有效行 Commit Valid Rows
        </button>
        <button class="outline" type="button" @click="emit('cancel')">
          取消 Cancel
        </button>
      </footer>
    </div>
  </section>
</template>

<style scoped>
.import-wizard {
  display: grid;
  gap: 12px;
}

header h3 {
  margin: 0;
}

header p {
  margin: 6px 0 0;
  color: var(--color-text-soft);
}

.dropzone {
  border: 2px dashed #9fc8d7;
  padding: 18px;
  text-align: center;
  display: grid;
  gap: 4px;
  cursor: pointer;
  background: #f3fbff;
}

.dropzone input {
  display: none;
}

.progress {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.progress span {
  width: 60px;
  height: 10px;
  background: linear-gradient(90deg, #4ea1ce 0%, #69d4ad 100%);
  border-radius: 999px;
  animation: progress 1s infinite linear;
}

.preview {
  padding: 12px;
  display: grid;
  gap: 10px;
}

.preview-head {
  display: flex;
  justify-content: space-between;
}

.table-wrap {
  overflow: auto;
  border: 1px solid var(--color-border);
  border-radius: 10px;
}

table {
  width: 100%;
  border-collapse: collapse;
  min-width: 850px;
}

th,
td {
  border-bottom: 1px solid var(--color-border);
  text-align: left;
  padding: 8px;
}

.status {
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 0.75rem;
  text-transform: uppercase;
}

.status.valid {
  background: #e6f8ef;
  color: var(--color-success);
}

.status.invalid {
  background: #fdeceb;
  color: var(--color-danger);
}

.status.duplicate {
  background: #fff5e8;
  color: var(--color-warning);
}

.actions {
  display: flex;
  gap: 8px;
}

.primary,
.outline {
  min-height: 38px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  padding: 0 14px;
}

.primary {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: white;
}

.outline {
  background: white;
}

@keyframes progress {
  0% {
    transform: translateX(0);
    opacity: 0.9;
  }
  50% {
    transform: translateX(6px);
    opacity: 1;
  }
  100% {
    transform: translateX(0);
    opacity: 0.9;
  }
}
</style>
