<script setup lang="ts">
import { computed, ref, watch } from "vue";

interface VersionOption {
  id: number;
  label: string;
}

interface FieldDiff {
  fieldName: string;
  oldValue: string;
  newValue: string;
}

const props = defineProps<{
  versions: VersionOption[];
  fetchDiff: (leftId: number, rightId: number) => Promise<FieldDiff[]>;
  allowRestore: boolean;
}>();

const emit = defineEmits<{
  restore: [versionId: number];
}>();

const leftVersionId = ref<number>(0);
const rightVersionId = ref<number>(0);
const loading = ref(false);
const diffs = ref<FieldDiff[]>([]);

const hasSelection = computed(
  () => leftVersionId.value > 0 && rightVersionId.value > 0,
);

watch(
  () => props.versions,
  (versions) => {
    if (versions.length >= 2) {
      leftVersionId.value = versions[versions.length - 2].id;
      rightVersionId.value = versions[versions.length - 1].id;
      void loadDiff();
    }
  },
  { immediate: true },
);

async function loadDiff(): Promise<void> {
  if (!hasSelection.value) {
    return;
  }

  loading.value = true;
  try {
    diffs.value = await props.fetchDiff(
      leftVersionId.value,
      rightVersionId.value,
    );
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <section class="diff-viewer card">
    <header class="controls">
      <div class="selectors">
        <label>
          左侧版本 Left
          <select v-model.number="leftVersionId" @change="loadDiff">
            <option
              v-for="version in versions"
              :key="`left-${version.id}`"
              :value="version.id"
            >
              {{ version.label }}
            </option>
          </select>
        </label>

        <label>
          右侧版本 Right
          <select v-model.number="rightVersionId" @change="loadDiff">
            <option
              v-for="version in versions"
              :key="`right-${version.id}`"
              :value="version.id"
            >
              {{ version.label }}
            </option>
          </select>
        </label>
      </div>

      <button
        v-if="allowRestore"
        class="restore-btn"
        type="button"
        @click="emit('restore', leftVersionId)"
      >
        恢复左侧版本 Restore This Version
      </button>
    </header>

    <div v-if="loading" class="loading">Loading...</div>
    <div v-else-if="diffs.length === 0" class="empty">
      无差异 No differences
    </div>
    <div v-else class="diff-list">
      <article v-for="diff in diffs" :key="diff.fieldName" class="diff-item">
        <h4>{{ diff.fieldName }}</h4>
        <div class="row">
          <div class="old">
            <strong>旧值 Old</strong>
            <p>{{ diff.oldValue || "-" }}</p>
          </div>
          <div class="new">
            <strong>新值 New</strong>
            <p>{{ diff.newValue || "-" }}</p>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.diff-viewer {
  padding: 14px;
  display: grid;
  gap: 12px;
}

.controls {
  display: flex;
  justify-content: space-between;
  align-items: end;
  flex-wrap: wrap;
  gap: 10px;
}

.selectors {
  display: flex;
  gap: 10px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

select,
.restore-btn {
  min-height: 38px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  padding: 0 10px;
}

.restore-btn {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
}

.diff-list {
  display: grid;
  gap: 10px;
}

.diff-item {
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 10px;
}

.diff-item h4 {
  margin: 0 0 8px;
}

.row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.old,
.new {
  border-radius: 8px;
  padding: 8px;
}

.old {
  background: #fdeeed;
  color: #9e3a35;
}

.new {
  background: #e8f8ef;
  color: #2e7b49;
}

.old p,
.new p {
  margin: 4px 0 0;
  white-space: pre-wrap;
}

.empty,
.loading {
  color: var(--color-text-soft);
}

@media (max-width: 760px) {
  .row {
    grid-template-columns: 1fr;
  }
}
</style>
