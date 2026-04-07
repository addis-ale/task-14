<script setup lang="ts">
import { onMounted, ref } from "vue";
import { api, unwrap } from "@/api";
import { handleApiError } from "@/utils/toast";

const loading = ref(false);
const loadError = ref("");
const exams = ref<Array<Record<string, unknown>>>([]);

async function fetchExams(): Promise<void> {
  loading.value = true;
  loadError.value = "";
  try {
    exams.value = await unwrap(api.get("/my/exams"));
  } catch (err) {
    loadError.value = "加载失败，请重试 Failed to load exams";
    handleApiError(err);
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void fetchExams();
});
</script>

<template>
  <section class="page-grid">
    <header>
      <h2>我的考试安排</h2>
      <p>Upcoming exam calendar for students</p>
    </header>

    <div v-if="loading" class="card loading">加载中 Loading...</div>

    <div v-else-if="loadError" class="card error-state">
      <p>{{ loadError }}</p>
      <button type="button" @click="fetchExams">重试 Retry</button>
    </div>

    <div v-else-if="exams.length === 0" class="card loading">
      暂无考试安排 No exam schedule
    </div>

    <ul v-else class="calendar-grid">
      <li
        v-for="item in exams"
        :key="`${item.sessionId}-${item.date}`"
        class="card exam-card"
      >
        <strong>{{ item.subject || "Subject" }}</strong>
        <span
          >{{ item.date }} {{ item.startTime }} - {{ item.endTime }}</span
        >
        <small
          >考场 Room: {{ item.room || "-" }} · 座位 Seat:
          {{ item.seat || "-" }}</small
        >
      </li>
    </ul>
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

p,
span,
small {
  color: var(--color-text-soft);
}

.loading {
  padding: 14px;
}

.error-state {
  padding: 14px;
  background: #fdeeed;
  border-color: #f7c6c2;
  color: #9e3a35;
  display: grid;
  gap: 10px;
}

.error-state p {
  margin: 0;
  color: inherit;
}

.error-state button {
  width: fit-content;
  min-height: 36px;
  border-radius: 10px;
  border: 1px solid #f7c6c2;
  background: white;
  color: #9e3a35;
  padding: 0 14px;
  font: inherit;
  cursor: pointer;
}

.calendar-grid {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.exam-card {
  padding: 14px;
  display: grid;
  gap: 6px;
}
</style>
