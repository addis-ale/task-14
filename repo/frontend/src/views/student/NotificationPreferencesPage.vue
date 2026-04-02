<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { api, unwrap } from "@/api";

const loading = ref(false);
const saving = ref(false);
const message = ref("");

const form = reactive<{
  preferences: Array<{ eventType: string; enabled: boolean }>;
  dndStart: string;
  dndEnd: string;
}>({
  preferences: [],
  dndStart: "22:00",
  dndEnd: "07:00",
});

onMounted(async () => {
  loading.value = true;
  try {
    const data = await unwrap(api.get("/notification-preferences"));
    form.preferences = data.preferences || [];
    form.dndStart = data.dndStart || form.dndStart;
    form.dndEnd = data.dndEnd || form.dndEnd;
  } finally {
    loading.value = false;
  }
});

async function save(): Promise<void> {
  saving.value = true;
  message.value = "";
  try {
    await unwrap(
      api.put("/notification-preferences", {
        preferences: form.preferences,
        dndStart: form.dndStart,
        dndEnd: form.dndEnd,
      }),
    );
    message.value = "偏好已保存 Preferences saved";
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <section class="page-grid">
    <header>
      <h2>通知偏好</h2>
      <p>Per-event toggle and DND time range</p>
    </header>

    <div class="card form-card" v-if="loading">加载中 Loading...</div>

    <div class="card form-card" v-else>
      <ul>
        <li v-for="item in form.preferences" :key="item.eventType">
          <label>
            <span>{{ item.eventType }}</span>
            <input v-model="item.enabled" type="checkbox" />
          </label>
        </li>
      </ul>

      <div class="dnd-row">
        <label>
          DND 开始
          <input v-model="form.dndStart" type="time" />
        </label>
        <label>
          DND 结束
          <input v-model="form.dndEnd" type="time" />
        </label>
      </div>

      <button type="button" @click="save" :disabled="saving">
        {{ saving ? "保存中..." : "保存 Save" }}
      </button>
      <p v-if="message" class="hint">{{ message }}</p>
    </div>
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

p {
  margin: 5px 0 0;
  color: var(--color-text-soft);
}

.form-card {
  padding: 14px;
  display: grid;
  gap: 12px;
  max-width: 680px;
}

ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 8px;
}

label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 8px 10px;
}

.dnd-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 8px;
}

.dnd-row label {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 6px;
}

input[type="time"],
button {
  min-height: 38px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  padding: 0 10px;
}

button {
  width: fit-content;
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: white;
}

.hint {
  margin: 0;
}
</style>
