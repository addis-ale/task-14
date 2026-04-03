<script setup lang="ts">
import { reactive, ref, computed } from "vue";
import DataTable from "@/components/DataTable/DataTable.vue";
import { api, unwrap } from "@/api";
import { handleApiError, showSuccess } from "@/utils/toast";
import { formatDateTime } from "@/utils/date";
import { useI18n } from "@/i18n";
import { useRBAC } from "@/composables/useRBAC";
import type { TableColumn } from "@/types/ui";
import type { PageData } from "@/types/api";

const { can } = useRBAC();

const { t } = useI18n();
const activeTab = ref("PENDING");
const filters = reactive({ status: "PENDING" });
const tableKey = ref(0);

const tabs = [
  { value: "PENDING", label: t("compliance.pending") },
  { value: "APPROVED", label: t("compliance.approved") },
  { value: "REJECTED", label: t("compliance.rejected") },
];

const columns: TableColumn[] = [
  { key: "id", label: "ID", sortable: true, width: "70px" },
  { key: "contentType", label: "内容类型 Content Type", sortable: true },
  { key: "title", label: "标题 Title", sortable: true },
  { key: "status", label: "状态 Status", sortable: true },
  { key: "createdAt", label: "创建时间 Created", sortable: true },
];

// Review detail state
const showDetail = ref(false);
const selectedReview = ref<Record<string, unknown> | null>(null);
const reviewComments = ref("");
const showConfirmDialog = ref(false);
const confirmAction = ref<"approve" | "reject">("approve");

const pendingCount = ref(0);

async function fetcher(
  params: Record<string, unknown>,
): Promise<PageData<Record<string, unknown>>> {
  const result = await unwrap(
    api.get("/compliance-reviews", { params: { ...params, ...filters } }),
  );
  if (filters.status === "PENDING") {
    pendingCount.value = result.pagination?.totalItems || result.items?.length || 0;
  }
  return result;
}

function switchTab(tab: string) {
  activeTab.value = tab;
  filters.status = tab;
  tableKey.value++;
}

async function openDetail(row: Record<string, unknown>) {
  try {
    selectedReview.value = await unwrap(api.get(`/compliance-reviews/${row.id}`));
    showDetail.value = true;
    reviewComments.value = "";
  } catch (err) {
    handleApiError(err);
  }
}

function startApprove() {
  confirmAction.value = "approve";
  showConfirmDialog.value = true;
}

function startReject() {
  confirmAction.value = "reject";
  showConfirmDialog.value = true;
}

async function executeDecision(): Promise<void> {
  if (!selectedReview.value) return;
  try {
    const endpoint = confirmAction.value === "approve" ? "approve" : "reject";
    await unwrap(
      api.post(`/compliance-reviews/${selectedReview.value.id}/${endpoint}`, {
        comments: reviewComments.value,
      }),
    );
    showSuccess(t("common.success"));
    showConfirmDialog.value = false;
    showDetail.value = false;
    tableKey.value++;
  } catch (err) {
    handleApiError(err);
  }
}

const statusClass = computed(() => (status: unknown) => {
  switch (status) {
    case "PENDING": return "badge-yellow";
    case "APPROVED": return "badge-green";
    case "REJECTED": return "badge-red";
    default: return "badge-gray";
  }
});
</script>

<template>
  <section class="page-grid">
    <nav class="breadcrumb" aria-label="Breadcrumb">
      <span>{{ t("nav.dashboard") }}</span> / <span>{{ t("nav.compliance") }}</span>
    </nav>

    <header>
      <h2>{{ t("compliance.title") }}</h2>
      <p>{{ t("compliance.subtitle") }}</p>
    </header>

    <!-- Status tabs -->
    <div class="tab-bar" role="tablist">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        type="button"
        role="tab"
        class="tab-btn"
        :class="{ active: activeTab === tab.value }"
        :aria-selected="activeTab === tab.value"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
        <span v-if="tab.value === 'PENDING' && pendingCount > 0" class="count-badge">
          {{ pendingCount }}
        </span>
      </button>
    </div>

    <DataTable
      :key="tableKey"
      :columns="columns"
      :fetcher="fetcher"
      :filters="filters"
      @row-click="openDetail"
    >
      <template #cell-status="{ row }">
        <span class="status-badge" :class="statusClass(row.status)">
          {{ row.status }}
        </span>
      </template>
      <template #cell-createdAt="{ row }">
        {{ formatDateTime(row.createdAt as string) }}
      </template>
      <template #actions="{ row }">
        <button type="button" @click.stop="openDetail(row)">查看 View</button>
      </template>
    </DataTable>

    <!-- Review Detail Modal -->
    <div v-if="showDetail && selectedReview" class="modal-backdrop" role="dialog" aria-modal="true">
      <div class="modal card">
        <header class="modal-header">
          <h3>{{ t("compliance.reviewDetail") }} #{{ selectedReview.id }}</h3>
          <button type="button" class="outline-btn" @click="showDetail = false">{{ t("common.close") }}</button>
        </header>

        <div class="review-content">
          <section class="preview-section">
            <h4>{{ t("compliance.contentPreview") }}</h4>
            <div class="preview-card">
              <strong>{{ selectedReview.title || selectedReview.contentTitle || '-' }}</strong>
              <p>{{ selectedReview.body || selectedReview.contentBody || '-' }}</p>
            </div>
          </section>

          <section class="scope-section">
            <h4>{{ t("compliance.targetScope") }}</h4>
            <div class="scope-info">
              <span v-if="selectedReview.gradeIds">年级 Grade: {{ selectedReview.gradeIds }}</span>
              <span v-if="selectedReview.classIds">班级 Class: {{ selectedReview.classIds }}</span>
              <span v-if="selectedReview.subjectIds">科目 Subject: {{ selectedReview.subjectIds }}</span>
              <span v-if="!selectedReview.gradeIds && !selectedReview.classIds">-</span>
            </div>
          </section>

          <section v-if="selectedReview.status === 'PENDING' && can('review')" class="action-section">
            <label class="field">
              <span>{{ t("compliance.comments") }}</span>
              <textarea
                v-model="reviewComments"
                :placeholder="t('compliance.commentsPlaceholder')"
                rows="3"
              />
            </label>
            <div class="action-buttons">
              <button type="button" class="approve-btn" @click="startApprove">
                {{ t("common.approve") }}
              </button>
              <button type="button" class="reject-btn" @click="startReject">
                {{ t("common.reject") }}
              </button>
            </div>
          </section>
        </div>
      </div>
    </div>

    <!-- Confirmation Dialog -->
    <div v-if="showConfirmDialog" class="modal-backdrop confirm-layer" role="dialog" aria-modal="true">
      <div class="modal card confirm-modal">
        <h3>{{ t("common.confirm") }}</h3>
        <p>
          {{ confirmAction === 'approve' ? t("compliance.confirmApprove") : t("compliance.confirmReject") }}
        </p>
        <div class="modal-actions">
          <button
            type="button"
            :class="confirmAction === 'approve' ? 'approve-btn' : 'reject-btn'"
            @click="executeDecision"
          >
            {{ t("common.confirm") }}
          </button>
          <button type="button" class="outline-btn" @click="showConfirmDialog = false">
            {{ t("common.cancel") }}
          </button>
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

h2, h3, h4 {
  margin: 0;
}

p {
  margin: 5px 0 0;
  color: var(--color-text-soft);
}

.tab-bar {
  display: flex;
  gap: 4px;
  border-bottom: 2px solid var(--color-border);
  padding-bottom: 0;
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
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tab-btn.active {
  border-bottom-color: var(--color-primary);
  color: var(--color-primary);
  font-weight: 600;
}

.count-badge {
  background: var(--color-danger);
  color: white;
  font-size: 0.72rem;
  padding: 1px 6px;
  border-radius: 999px;
  font-weight: 600;
}

.status-badge {
  font-size: 0.8rem;
  padding: 2px 10px;
  border-radius: 999px;
  font-weight: 500;
}

.badge-green { background: #e8f8ef; color: #2d8f57; }
.badge-red { background: #fdeeed; color: #9e3a35; }
.badge-yellow { background: #fef9e7; color: #b7791f; }
.badge-gray { background: #f0f2f4; color: #6b7280; }

button {
  min-height: 36px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  padding: 0 10px;
  font: inherit;
  cursor: pointer;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(25, 40, 50, 0.32);
  display: grid;
  place-items: center;
  z-index: 20;
}

.confirm-layer {
  z-index: 25;
}

.modal {
  width: min(600px, calc(100vw - 28px));
  padding: 18px;
  max-height: 90vh;
  overflow: auto;
}

.confirm-modal {
  width: min(400px, calc(100vw - 28px));
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.review-content {
  display: grid;
  gap: 14px;
}

.preview-section h4,
.scope-section h4 {
  margin-bottom: 8px;
  color: var(--color-text-soft);
  font-size: 0.85rem;
}

.preview-card {
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 12px;
  background: #f9fbfc;
}

.preview-card strong {
  display: block;
  margin-bottom: 6px;
}

.preview-card p {
  margin: 0;
  white-space: pre-wrap;
  color: var(--color-text);
}

.scope-info {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

textarea {
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 8px 10px;
  font: inherit;
  resize: vertical;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.approve-btn {
  background: var(--color-success);
  border-color: var(--color-success);
  color: white;
}

.reject-btn {
  background: #fdeeed;
  border-color: #f7c6c2;
  color: #9e3a35;
}

.outline-btn {
  background: white;
}

.modal-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}
</style>
