<script setup lang="ts">
import { reactive, ref } from "vue";
import DataTable from "@/components/DataTable/DataTable.vue";
import { api, unwrap } from "@/api";
import { handleApiError, showSuccess } from "@/utils/toast";
import { formatDateTime } from "@/utils/date";
import { maskStudentId } from "@/utils/pii";
import { useI18n } from "@/i18n";
import { useRBAC } from "@/composables/useRBAC";
import type { TableColumn } from "@/types/ui";
import type { PageData } from "@/types/api";

const { can } = useRBAC();

const { t } = useI18n();
const activeTab = ref("PENDING");
const filters = reactive({ ruleType: "", status: "PENDING" });
const tableKey = ref(0);

const tabs = [
  { value: "PENDING", label: t("antiCheat.pending") },
  { value: "CLEARED", label: t("antiCheat.cleared") },
  { value: "CONFIRMED_FRAUD", label: t("antiCheat.confirmedFraud") },
];

const columns: TableColumn[] = [
  { key: "id", label: "ID", sortable: true, width: "70px" },
  { key: "studentName", label: "学生姓名 Student", sortable: true },
  { key: "ruleType", label: "标记类型 Flag Type", sortable: true },
  { key: "status", label: "审核状态 Status", sortable: true },
  { key: "riskScore", label: "风险分 Risk", sortable: true },
  { key: "createdAt", label: "发现时间 Created", sortable: true },
];

const pendingCount = ref(0);

// Detail state
const showDetail = ref(false);
const selectedFlag = ref<Record<string, unknown> | null>(null);
const decision = ref<"CLEARED" | "CONFIRMED_FRAUD">("CLEARED");
const comments = ref("");

async function fetcher(
  params: Record<string, unknown>,
): Promise<PageData<Record<string, unknown>>> {
  const result = await unwrap(
    api.get("/anti-cheat/flags", { params: { ...params, ...filters } }),
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
    selectedFlag.value = await unwrap(api.get(`/anti-cheat/flags/${row.id}`));
    showDetail.value = true;
    decision.value = "CLEARED";
    comments.value = "";
  } catch (err) {
    handleApiError(err);
  }
}

async function submitDecision(): Promise<void> {
  if (!selectedFlag.value) return;
  try {
    await unwrap(
      api.post(`/anti-cheat/flags/${selectedFlag.value.id}/review`, {
        decision: decision.value,
        comment: comments.value,
      }),
    );
    showSuccess(t("common.success"));
    showDetail.value = false;
    tableKey.value++;
  } catch (err) {
    handleApiError(err);
  }
}

function statusClass(status: unknown): string {
  switch (status) {
    case "PENDING": return "badge-yellow";
    case "CLEARED": return "badge-green";
    case "CONFIRMED_FRAUD": return "badge-red";
    default: return "badge-gray";
  }
}

function flagTypeLabel(type: unknown): string {
  switch (type) {
    case "ACTIVITY_BURST": return t("antiCheat.activityBurst");
    case "IDENTICAL_SUBMISSIONS": return t("antiCheat.identicalSubmissions");
    case "ABNORMAL_SCORE_DELTA": return t("antiCheat.abnormalScoreDelta");
    default: return String(type || "-");
  }
}

// Visualization data accessors — return null when backend data is absent
function getTimelineData(flag: Record<string, unknown>): number[] | null {
  const data = flag.activityData as number[] | undefined;
  return data && data.length > 0 ? data : null;
}

function getSubmissionPair(flag: Record<string, unknown>): { left: string; right: string } | null {
  const pair = flag.submissionPair as { left?: string; right?: string } | undefined;
  if (!pair || (!pair.left && !pair.right)) return null;
  return { left: pair.left || "", right: pair.right || "" };
}

function getScoreData(flag: Record<string, unknown>): { student: number; classAvg: number; classStd: number } | null {
  const data = flag.scoreData as { student?: number; classAvg?: number; classStd?: number } | undefined;
  if (!data || data.student === undefined) return null;
  return {
    student: data.student,
    classAvg: data.classAvg ?? 0,
    classStd: data.classStd ?? 0,
  };
}
</script>

<template>
  <section class="page-grid">
    <nav class="breadcrumb" aria-label="Breadcrumb">
      <span>{{ t("nav.dashboard") }}</span> / <span>{{ t("nav.antiCheat") }}</span>
    </nav>

    <header>
      <h2>{{ t("antiCheat.title") }}</h2>
      <p>{{ t("antiCheat.subtitle") }}</p>
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
      <template #cell-ruleType="{ row }">
        {{ flagTypeLabel(row.ruleType) }}
      </template>
      <template #cell-status="{ row }">
        <span class="status-badge" :class="statusClass(row.status)">{{ row.status }}</span>
      </template>
      <template #cell-createdAt="{ row }">
        {{ formatDateTime(row.createdAt as string) }}
      </template>
      <template #actions="{ row }">
        <button type="button" @click.stop="openDetail(row)">查看 View</button>
      </template>
    </DataTable>

    <!-- Review Detail Modal -->
    <div v-if="showDetail && selectedFlag" class="modal-backdrop" role="dialog" aria-modal="true">
      <div class="modal card detail-modal">
        <header class="modal-header">
          <h3>{{ t("antiCheat.flagDetails") }} #{{ selectedFlag.id }}</h3>
          <button type="button" class="outline-btn" @click="showDetail = false">{{ t("common.close") }}</button>
        </header>

        <!-- Student Info -->
        <section class="info-section">
          <h4>{{ t("antiCheat.studentInfo") }}</h4>
          <div class="info-grid">
            <div><strong>{{ t("antiCheat.studentName") }}</strong><span>{{ selectedFlag.studentName || selectedFlag.targetRef || '-' }}</span></div>
            <div><strong>学号 Student ID</strong><span>{{ maskStudentId(selectedFlag.studentId as string | number | undefined) }}</span></div>
            <div><strong>{{ t("antiCheat.flagType") }}</strong><span>{{ flagTypeLabel(selectedFlag.ruleType) }}</span></div>
            <div><strong>风险分 Risk Score</strong><span class="risk-score">{{ selectedFlag.riskScore || '-' }}</span></div>
          </div>
        </section>

        <!-- Flag Visualization -->
        <section class="viz-section">
          <h4>{{ t("antiCheat.flagDetails") }}</h4>

          <!-- ACTIVITY_BURST: Timeline chart -->
          <div v-if="selectedFlag.ruleType === 'ACTIVITY_BURST'" class="viz-card">
            <h5>{{ t("antiCheat.actionDensity") }}</h5>
            <template v-if="getTimelineData(selectedFlag)">
              <div class="timeline-chart">
                <div
                  v-for="(val, idx) in getTimelineData(selectedFlag)"
                  :key="idx"
                  class="bar"
                  :style="{ height: Math.min(val * 4, 100) + 'px' }"
                  :class="{ highlight: val > 10 }"
                  :title="`T${idx}: ${val} actions`"
                />
              </div>
              <div class="chart-label">
                <small>时间区间 Time intervals</small>
              </div>
            </template>
            <div v-else class="data-unavailable">
              <strong>后端数据缺失 Backend data not available</strong>
              <p>活动数据未从服务端返回，无法显示时间线图表。Activity timeline data was not returned by the server.</p>
            </div>
          </div>

          <!-- IDENTICAL_SUBMISSIONS: Side-by-side -->
          <div v-if="selectedFlag.ruleType === 'IDENTICAL_SUBMISSIONS'" class="viz-card">
            <h5>{{ t("antiCheat.submissionComparison") }}</h5>
            <template v-if="getSubmissionPair(selectedFlag)">
              <div class="comparison-grid">
                <div class="submission-panel">
                  <strong>提交 A Submission A</strong>
                  <pre>{{ getSubmissionPair(selectedFlag)!.left }}</pre>
                </div>
                <div class="submission-panel">
                  <strong>提交 B Submission B</strong>
                  <pre>{{ getSubmissionPair(selectedFlag)!.right }}</pre>
                </div>
              </div>
            </template>
            <div v-else class="data-unavailable">
              <strong>后端数据缺失 Backend data not available</strong>
              <p>提交对比数据未从服务端返回，无法显示对比视图。Submission comparison data was not returned by the server.</p>
            </div>
          </div>

          <!-- ABNORMAL_SCORE_DELTA: Score chart -->
          <div v-if="selectedFlag.ruleType === 'ABNORMAL_SCORE_DELTA'" class="viz-card">
            <h5>{{ t("antiCheat.scoreDistribution") }}</h5>
            <template v-if="getScoreData(selectedFlag)">
            <div class="score-chart">
              <div class="score-bar-group">
                <div class="score-bar-wrapper">
                  <div class="score-bar student-bar" :style="{ height: getScoreData(selectedFlag)!.student + '%' }">
                    <span class="bar-label">{{ getScoreData(selectedFlag)!.student }}</span>
                  </div>
                  <small>该生 Student</small>
                </div>
                <div class="score-bar-wrapper">
                  <div class="score-bar avg-bar" :style="{ height: getScoreData(selectedFlag)!.classAvg + '%' }">
                    <span class="bar-label">{{ getScoreData(selectedFlag)!.classAvg }}</span>
                  </div>
                  <small>班均 Class Avg</small>
                </div>
                <div class="score-bar-wrapper">
                  <div class="score-bar std-bar" :style="{ height: getScoreData(selectedFlag)!.classStd * 3 + '%' }">
                    <span class="bar-label">&sigma;{{ getScoreData(selectedFlag)!.classStd }}</span>
                  </div>
                  <small>标准差 StdDev</small>
                </div>
              </div>
            </div>
            </template>
            <div v-else class="data-unavailable">
              <strong>后端数据缺失 Backend data not available</strong>
              <p>分数分布数据未从服务端返回，无法显示图表。Score distribution data was not returned by the server.</p>
            </div>
          </div>

          <!-- Fallback for unknown types -->
          <div v-if="!['ACTIVITY_BURST', 'IDENTICAL_SUBMISSIONS', 'ABNORMAL_SCORE_DELTA'].includes(String(selectedFlag.ruleType))" class="viz-card">
            <pre>{{ JSON.stringify(selectedFlag.details || selectedFlag, null, 2) }}</pre>
          </div>
        </section>

        <!-- Decision form -->
        <section v-if="selectedFlag.status === 'PENDING' && can('review')" class="decision-section">
          <h4>{{ t("antiCheat.decision") }}</h4>
          <p class="notice">{{ t("antiCheat.humanReviewOnly") }}</p>
          <div class="radio-group">
            <label class="radio-item">
              <input type="radio" v-model="decision" value="CLEARED" />
              {{ t("antiCheat.decisionCleared") }}
            </label>
            <label class="radio-item">
              <input type="radio" v-model="decision" value="CONFIRMED_FRAUD" />
              {{ t("antiCheat.decisionFraud") }}
            </label>
          </div>
          <label class="field">
            <span>{{ t("compliance.comments") }}</span>
            <textarea
              v-model="comments"
              :placeholder="t('antiCheat.commentsPlaceholder')"
              rows="3"
            />
          </label>
          <button type="button" class="primary-btn" @click="submitDecision">
            {{ t("common.submit") }}
          </button>
        </section>
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

.data-unavailable {
  padding: 16px;
  text-align: center;
  color: var(--color-warning);
  background: #fef9e7;
  border: 1px dashed #f0d78c;
  border-radius: 8px;
  font-size: 0.88rem;
}

.data-unavailable strong {
  display: block;
  margin-bottom: 4px;
}

.data-unavailable p {
  margin: 0;
  color: var(--color-text-soft);
  font-size: 0.82rem;
}

h2, h3, h4, h5 {
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

.detail-modal {
  width: min(720px, calc(100vw - 28px));
  padding: 18px;
  max-height: 90vh;
  overflow: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.info-section, .viz-section, .decision-section {
  margin-top: 14px;
}

.info-section h4, .viz-section h4, .decision-section h4 {
  margin-bottom: 8px;
  color: var(--color-text-soft);
  font-size: 0.85rem;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.info-grid > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-grid strong {
  font-size: 0.82rem;
  color: var(--color-text-soft);
}

.risk-score {
  font-weight: 700;
  color: var(--color-danger);
}

.viz-card {
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 12px;
  background: #f9fbfc;
}

.viz-card h5 {
  margin-bottom: 10px;
  font-size: 0.85rem;
}

/* Timeline chart */
.timeline-chart {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 100px;
  padding: 4px 0;
}

.bar {
  flex: 1;
  background: var(--color-primary);
  border-radius: 3px 3px 0 0;
  min-height: 4px;
  transition: height 0.3s;
}

.bar.highlight {
  background: var(--color-danger);
}

.chart-label {
  text-align: center;
  margin-top: 4px;
}

/* Side-by-side comparison */
.comparison-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.submission-panel {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 8px;
}

.submission-panel strong {
  display: block;
  margin-bottom: 6px;
  font-size: 0.82rem;
}

.submission-panel pre {
  margin: 0;
  font-size: 0.8rem;
  white-space: pre-wrap;
  background: transparent;
}

/* Score chart */
.score-chart {
  padding: 10px 0;
}

.score-bar-group {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: 24px;
  height: 120px;
}

.score-bar-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.score-bar {
  width: 40px;
  border-radius: 4px 4px 0 0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 4px;
  min-height: 10px;
}

.bar-label {
  font-size: 0.72rem;
  font-weight: 600;
  color: white;
}

.student-bar { background: var(--color-danger); }
.avg-bar { background: var(--color-primary); }
.std-bar { background: var(--color-warning); }

/* Decision form */
.notice {
  font-size: 0.85rem;
  color: var(--color-warning);
  font-weight: 500;
  margin: 4px 0 10px;
}

.radio-group {
  display: flex;
  gap: 16px;
  margin-bottom: 10px;
}

.radio-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.radio-item input[type="radio"] {
  min-height: auto;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 10px;
}

textarea {
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 8px 10px;
  font: inherit;
  resize: vertical;
}

.primary-btn {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
}

.outline-btn {
  background: white;
}

@media (max-width: 640px) {
  .comparison-grid {
    grid-template-columns: 1fr;
  }
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
