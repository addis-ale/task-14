<script setup lang="ts">
import { onMounted, ref } from "vue";
import { api, unwrap } from "@/api";

const loading = ref(false);
const exams = ref<Array<Record<string, unknown>>>([]);

onMounted(async () => {
  loading.value = true;
  try {
    exams.value = await unwrap(api.get("/my/exams"));
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <section class="page-grid">
    <header>
      <h2>我的考试安排</h2>
      <p>Upcoming exam calendar for students</p>
    </header>

    <div v-if="loading" class="card loading">加载中 Loading...</div>

    <div v-else-if="exams.length === 0" class="card loading">
      暂无考试安排 No exam schedule
    </div>

    <ul v-else class="calendar-grid">
      <li
        v-for="item in exams"
        :key="`${item.sessionId}-${item.examDate}`"
        class="card exam-card"
      >
        <strong>{{ item.subjectName || "Subject" }}</strong>
        <span
          >{{ item.examDate }} {{ item.startTime }} - {{ item.endTime }}</span
        >
        <small
          >考场 Room: {{ item.roomName || "-" }} · 座位 Seat:
          {{ item.seatNo || "-" }}</small
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
