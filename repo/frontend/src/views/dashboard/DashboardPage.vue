<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import { api, unwrap } from "@/api";
import { formatDateTime } from "@/utils/date";
import { handleApiError } from "@/utils/toast";
import { useI18n } from "@/i18n";

const { t } = useI18n();

interface Term {
  id: number;
  name: string;
}

interface DashboardStats {
  totalSessions: number;
  upcomingSessions: number;
  totalStudents: number;
  pendingReviews: number;
  failedJobs: number;
  pendingAntiCheatFlags: number;
  recentActivity: Array<{ title: string; at: string }>;
}

const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const selectedTermId = ref<number | "">("");
const terms = ref<Term[]>([]);

const adminStats = ref<DashboardStats>({
  totalSessions: 0,
  upcomingSessions: 0,
  totalStudents: 0,
  pendingReviews: 0,
  failedJobs: 0,
  pendingAntiCheatFlags: 0,
  recentActivity: [],
});

const exams = ref<
  Array<{
    subjectName: string;
    examDate: string;
    startTime: string;
    roomName: string;
  }>
>([]);

const isStudent = computed(() => authStore.activeRole === "STUDENT");

async function loadTerms() {
  try {
    const data = await unwrap(api.get("/terms"));
    terms.value = data.items || data || [];
    if (terms.value.length > 0 && !selectedTermId.value) {
      selectedTermId.value = terms.value[0].id;
    }
  } catch {
    terms.value = [];
  }
}

async function loadDashboard() {
  loading.value = true;
  try {
    if (isStudent.value) {
      exams.value = await unwrap(api.get("/my/exams"));
    } else {
      const params: Record<string, unknown> = {};
      if (selectedTermId.value) params.termId = selectedTermId.value;
      adminStats.value = await unwrap(
        api.get("/dashboard/stats", { params }),
      );
    }
  } catch (err) {
    handleApiError(err);
    if (!isStudent.value) {
      adminStats.value = {
        totalSessions: 0,
        upcomingSessions: 0,
        totalStudents: 0,
        pendingReviews: 0,
        failedJobs: 0,
        pendingAntiCheatFlags: 0,
        recentActivity: [],
      };
    }
  } finally {
    loading.value = false;
  }
}

watch(selectedTermId, () => {
  if (!isStudent.value) void loadDashboard();
});

onMounted(async () => {
  if (!isStudent.value) await loadTerms();
  await loadDashboard();
});

const statCards = computed(() => [
  {
    key: "totalSessions",
    value: adminStats.value.totalSessions,
    label: t("dashboard.totalSessions"),
    color: "var(--color-primary)",
  },
  {
    key: "upcomingSessions",
    value: adminStats.value.upcomingSessions,
    label: t("dashboard.upcomingSessions"),
    color: "var(--color-accent)",
  },
  {
    key: "totalStudents",
    value: adminStats.value.totalStudents,
    label: t("dashboard.totalStudents"),
    color: "#6366f1",
  },
  {
    key: "pendingReviews",
    value: adminStats.value.pendingReviews,
    label: t("dashboard.pendingReviews"),
    color: "var(--color-warning)",
  },
  {
    key: "failedJobs",
    value: adminStats.value.failedJobs,
    label: t("dashboard.failedJobs"),
    color: "var(--color-danger)",
  },
  {
    key: "pendingAntiCheatFlags",
    value: adminStats.value.pendingAntiCheatFlags,
    label: t("dashboard.antiCheatFlags"),
    color: "#e040a0",
  },
]);
</script>

<template>
  <section class="dashboard">
    <nav class="breadcrumb" aria-label="Breadcrumb">
      <span>{{ t("nav.dashboard") }}</span>
    </nav>

    <header class="head-row">
      <div>
        <h2>{{ t("dashboard.welcome") }}</h2>
        <p>{{ t("dashboard.subtitle") }}</p>
      </div>
      <select
        v-if="!isStudent && terms.length > 0"
        v-model="selectedTermId"
        class="term-select"
        aria-label="Select term"
      >
        <option v-for="term in terms" :key="term.id" :value="term.id">
          {{ term.name }}
        </option>
      </select>
    </header>

    <!-- Skeleton loader -->
    <div v-if="loading" class="stats-grid">
      <article v-for="i in 6" :key="i" class="card stat skeleton-card">
        <span class="skeleton skeleton-number" />
        <span class="skeleton skeleton-label" />
      </article>
    </div>

    <template v-else-if="!isStudent">
      <div class="stats-grid">
        <article
          v-for="card in statCards"
          :key="card.key"
          class="card stat"
        >
          <strong :style="{ color: card.color }">{{ card.value }}</strong>
          <span>{{ card.label }}</span>
        </article>
      </div>

      <!-- Quick action buttons -->
      <div class="quick-actions">
        <button
          type="button"
          class="action-btn primary-btn"
          @click="router.push('/scheduling/sessions/new')"
        >
          {{ t("dashboard.createSession") }}
        </button>
        <button
          type="button"
          class="action-btn outline-btn"
          @click="router.push('/notifications/compliance-reviews')"
        >
          {{ t("dashboard.reviewQueue") }}
        </button>
        <button
          type="button"
          class="action-btn outline-btn"
          @click="router.push('/admin/jobs')"
        >
          {{ t("dashboard.jobMonitor") }}
        </button>
      </div>

      <section class="card timeline">
        <h3>{{ t("dashboard.recentActivity") }}</h3>
        <ul>
          <li
            v-for="item in adminStats.recentActivity"
            :key="`${item.title}-${item.at}`"
          >
            <strong>{{ item.title }}</strong>
            <small>{{ formatDateTime(item.at) }}</small>
          </li>
          <li v-if="adminStats.recentActivity.length === 0" class="empty-item">
            {{ t("dashboard.noActivity") }}
          </li>
        </ul>
      </section>
    </template>

    <section v-else class="card calendar">
      <h3>{{ t("dashboard.upcomingExams") }}</h3>
      <ul v-if="exams.length > 0">
        <li
          v-for="exam in exams"
          :key="`${exam.subjectName}-${exam.examDate}-${exam.startTime}`"
        >
          <strong>{{ exam.subjectName }}</strong>
          <span>{{ exam.examDate }} {{ exam.startTime }} · {{ exam.roomName }}</span>
        </li>
      </ul>
      <div v-else class="empty-state">
        <p class="illu">📋</p>
        <p>{{ t("dashboard.noExams") }}</p>
      </div>
    </section>
  </section>
</template>

<style scoped>
.dashboard {
  display: grid;
  gap: 14px;
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
  gap: 12px;
}

h2 {
  margin: 0;
}

header p {
  margin: 6px 0 0;
  color: var(--color-text-soft);
}

.term-select {
  min-height: 38px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  padding: 0 12px;
  background: white;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 10px;
}

.stat {
  padding: 14px;
  display: grid;
  gap: 8px;
}

.stat strong {
  font-size: 1.8rem;
}

.skeleton-card {
  min-height: 80px;
}

.skeleton {
  display: block;
  border-radius: 8px;
  background: linear-gradient(90deg, #f2f7fa 0%, #e3eef4 50%, #f2f7fa 100%);
  animation: pulse 1.2s infinite ease-in-out;
}

.skeleton-number {
  height: 28px;
  width: 60%;
}

.skeleton-label {
  height: 14px;
  width: 80%;
}

.quick-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.action-btn {
  min-height: 38px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  padding: 0 16px;
  cursor: pointer;
  font: inherit;
}

.primary-btn {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
}

.outline-btn {
  background: white;
}

.timeline,
.calendar {
  padding: 14px;
}

.timeline h3,
.calendar h3 {
  margin: 0 0 10px;
}

ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 8px;
}

li {
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.empty-item {
  color: var(--color-text-soft);
  text-align: center;
}

small,
span {
  color: var(--color-text-soft);
}

.empty-state {
  text-align: center;
  padding: 20px 0;
}

.illu {
  font-size: 2.4rem;
  margin: 0 0 4px;
}

.empty-state p:last-child {
  color: var(--color-text-soft);
}

@keyframes pulse {
  0% { opacity: 0.55; }
  50% { opacity: 1; }
  100% { opacity: 0.55; }
}
</style>
