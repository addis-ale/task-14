<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import VersionDiff from "@/components/VersionDiff/VersionDiff.vue";
import { api, unwrap } from "@/api";
import { useAuthStore } from "@/stores/auth";
import { handleApiError, showSuccess } from "@/utils/toast";
import { formatDateTime } from "@/utils/date";
import { useI18n } from "@/i18n";

const { t } = useI18n();
const route = useRoute();
const authStore = useAuthStore();
const id = Number(route.params.id);

const loading = ref(false);
const detail = ref<Record<string, unknown> | null>(null);
const versions = ref<Array<{ id: number; label: string }>>([]);
const activeTab = ref<"detail" | "history">("detail");
const showRestoreConfirm = ref(false);
const restoreVersionId = ref<number>(0);

const versionList = ref<
  Array<{
    id: number;
    versionNumber: number;
    changedBy: string;
    changedAt: string;
    snapshotPreview: string;
  }>
>([]);

const allowRestore = computed(() =>
  ["ADMIN", "ACADEMIC_AFFAIRS"].includes(authStore.activeRole),
);

async function loadDetail(): Promise<void> {
  loading.value = true;
  try {
    detail.value = await unwrap(api.get(`/sessions/${id}`));
    const history = await unwrap(
      api.get("/versions", {
        params: { entityType: "EXAM_SESSION", entityId: id },
      }),
    );
    const items = history.items || [];
    versions.value = items.map(
      (item: { id: number; versionNumber: number }) => ({
        id: item.id,
        label: `v${item.versionNumber}`,
      }),
    );
    versionList.value = items.map(
      (item: {
        id: number;
        versionNumber: number;
        changedBy: string;
        createdAt: string;
        snapshotJson: string;
      }) => ({
        id: item.id,
        versionNumber: item.versionNumber,
        changedBy: item.changedBy || "-",
        changedAt: item.createdAt || "",
        snapshotPreview:
          item.snapshotJson
            ? String(item.snapshotJson).substring(0, 80) + "..."
            : "-",
      }),
    );
  } catch (err) {
    handleApiError(err);
  } finally {
    loading.value = false;
  }
}

async function fetchDiff(leftId: number, rightId: number) {
  const data = await unwrap(
    api.get("/versions/compare", {
      params: { versionA: leftId, versionB: rightId },
    }),
  );
  return (data.diffs || []).map(
    (item: { field: string; oldValue: string; newValue: string }) => ({
      fieldName: item.field,
      oldValue: item.oldValue,
      newValue: item.newValue,
    }),
  );
}

function confirmRestore(versionId: number) {
  restoreVersionId.value = versionId;
  showRestoreConfirm.value = true;
}

async function executeRestore(): Promise<void> {
  try {
    await unwrap(api.post(`/versions/${restoreVersionId.value}/restore`));
    showSuccess(t("version.restoreSuccess"));
    showRestoreConfirm.value = false;
    await loadDetail();
  } catch (err) {
    handleApiError(err);
  }
}

onMounted(() => {
  void loadDetail();
});
</script>

<template>
  <section class="page-grid">
    <nav class="breadcrumb" aria-label="Breadcrumb">
      <RouterLink to="/dashboard">{{ t("nav.dashboard") }}</RouterLink> /
      <RouterLink to="/scheduling/sessions">{{ t("nav.scheduling") }}</RouterLink> /
      <span>#{{ id }}</span>
    </nav>

    <header>
      <h2>场次详情 #{{ id }}</h2>
      <p>Session detail and version timeline</p>
    </header>

    <!-- Tabs -->
    <div class="tab-bar" role="tablist">
      <button
        type="button"
        role="tab"
        class="tab-btn"
        :class="{ active: activeTab === 'detail' }"
        @click="activeTab = 'detail'"
      >详情 Detail</button>
      <button
        type="button"
        role="tab"
        class="tab-btn"
        :class="{ active: activeTab === 'history' }"
        @click="activeTab = 'history'"
      >{{ t("version.history") }}</button>
    </div>

    <!-- Loading skeleton -->
    <div v-if="loading" class="card skeleton-block">
      <span class="skeleton" v-for="i in 4" :key="i" />
    </div>

    <!-- Detail tab -->
    <template v-if="!loading && activeTab === 'detail'">
      <section class="card detail" v-if="detail">
        <h3>{{ detail.subjectName || "Exam Session" }}</h3>
        <dl>
          <div>
            <dt>学期 Term</dt>
            <dd>{{ detail.termName || "-" }}</dd>
          </div>
          <div>
            <dt>年级 Grade</dt>
            <dd>{{ detail.gradeName || "-" }}</dd>
          </div>
          <div>
            <dt>日期 Date</dt>
            <dd>{{ detail.examDate || "-" }}</dd>
          </div>
          <div>
            <dt>时间 Time</dt>
            <dd>{{ detail.startTime || "-" }} - {{ detail.endTime || "-" }}</dd>
          </div>
        </dl>
      </section>
    </template>

    <!-- History tab -->
    <template v-if="!loading && activeTab === 'history'">
      <!-- Version list -->
      <section class="card version-list-section" v-if="versionList.length > 0">
        <h4>{{ t("version.history") }}</h4>
        <div class="version-list">
          <div
            v-for="v in versionList"
            :key="v.id"
            class="version-item"
          >
            <div class="version-info">
              <strong>v{{ v.versionNumber }}</strong>
              <span>{{ v.changedBy }}</span>
              <small>{{ formatDateTime(v.changedAt) }}</small>
            </div>
            <small class="preview-text">{{ v.snapshotPreview }}</small>
          </div>
        </div>
      </section>

      <!-- Compare mode -->
      <VersionDiff
        v-if="versions.length >= 2"
        :versions="versions"
        :fetch-diff="fetchDiff"
        :allow-restore="allowRestore"
        @restore="confirmRestore"
      />

      <div v-if="versionList.length === 0" class="card empty-state">
        <p>暂无版本历史 No version history</p>
      </div>
    </template>

    <!-- Restore confirmation -->
    <div v-if="showRestoreConfirm" class="modal-backdrop" role="dialog" aria-modal="true">
      <div class="modal card">
        <h3>{{ t("version.restore") }}</h3>
        <p>{{ t("version.confirmRestore") }}</p>
        <div class="modal-actions">
          <button type="button" class="primary-btn" @click="executeRestore">{{ t("common.confirm") }}</button>
          <button type="button" class="outline-btn" @click="showRestoreConfirm = false">{{ t("common.cancel") }}</button>
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

.breadcrumb a {
  color: var(--color-primary);
  text-decoration: none;
}

h2, h3, h4 {
  margin: 0;
}

header p {
  color: var(--color-text-soft);
  margin: 4px 0 0;
}

.tab-bar {
  display: flex;
  gap: 4px;
  border-bottom: 2px solid var(--color-border);
}

.tab-btn {
  min-height: 38px;
  border: none;
  border-bottom: 2px solid transparent;
  background: none;
  padding: 0 16px;
  cursor: pointer;
  font: inherit;
  color: var(--color-text-soft);
  margin-bottom: -2px;
}

.tab-btn.active {
  border-bottom-color: var(--color-primary);
  color: var(--color-primary);
  font-weight: 600;
}

.skeleton-block {
  padding: 14px;
  display: grid;
  gap: 10px;
}

.skeleton {
  display: block;
  height: 16px;
  border-radius: 8px;
  background: linear-gradient(90deg, #f2f7fa 0%, #e3eef4 50%, #f2f7fa 100%);
  animation: pulse 1.2s infinite ease-in-out;
}

.detail {
  padding: 14px;
}

dl {
  margin: 12px 0 0;
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
}

dt {
  color: var(--color-text-soft);
  font-size: 0.85rem;
}

dd {
  margin: 2px 0 0;
}

.version-list-section {
  padding: 14px;
}

.version-list-section h4 {
  margin-bottom: 10px;
  color: var(--color-text-soft);
  font-size: 0.85rem;
}

.version-list {
  display: grid;
  gap: 6px;
}

.version-item {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 8px 10px;
  display: grid;
  gap: 4px;
}

.version-info {
  display: flex;
  gap: 10px;
  align-items: center;
}

.version-info strong {
  color: var(--color-primary);
}

.version-info small {
  color: var(--color-text-soft);
}

.preview-text {
  font-size: 0.8rem;
  color: var(--color-text-soft);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-state {
  padding: 20px;
  text-align: center;
  color: var(--color-text-soft);
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
  width: min(400px, calc(100vw - 28px));
  padding: 18px;
}

.modal h3 {
  margin-bottom: 8px;
}

.modal p {
  margin: 0 0 12px;
  color: var(--color-text-soft);
}

.modal-actions {
  display: flex;
  gap: 8px;
}

button {
  min-height: 36px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  padding: 0 12px;
  font: inherit;
  cursor: pointer;
}

.primary-btn {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
}

.outline-btn {
  background: white;
}

@keyframes pulse {
  0% { opacity: 0.55; }
  50% { opacity: 1; }
  100% { opacity: 0.55; }
}
</style>
