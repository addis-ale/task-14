<script setup lang="ts">
import { computed } from "vue";
import { useNotifications } from "@/composables/useNotifications";
import { formatDateTime } from "@/utils/date";

const { inbox, unread, loading, markRead, fetchInbox } = useNotifications();

const cards = computed(() => inbox.value?.items || []);

function priorityClass(priority: string): string {
  if (priority === "HIGH") {
    return "high";
  }
  if (priority === "MEDIUM") {
    return "medium";
  }
  return "low";
}
</script>

<template>
  <section class="page-grid">
    <nav class="breadcrumb" aria-label="Breadcrumb">
      <span>消息中心 Inbox</span>
    </nav>

    <header class="head-row">
      <div>
        <h2>消息中心</h2>
        <p>Student inbox with read state and priority markers</p>
      </div>
      <span class="badge">未读 {{ unread }}</span>
    </header>

    <div class="controls card">
      <button type="button" @click="fetchInbox(1, 20, false)">
        仅看未读 Unread
      </button>
      <button type="button" @click="fetchInbox()">查看全部 All</button>
    </div>

    <div v-if="loading" class="card empty">加载中 Loading...</div>

    <div v-else-if="cards.length === 0" class="card empty">
      <p class="illu">( )</p>
      <p>暂无消息 Empty inbox</p>
    </div>

    <ul v-else class="card-list">
      <li
        v-for="item in cards"
        :key="item.deliveryId"
        class="card inbox-card"
        :class="{ unread: !item.read }"
      >
        <div class="title-row">
          <span class="dot" :class="priorityClass(item.priority)" />
          <h3>{{ item.title }}</h3>
          <small>{{ item.priority }}</small>
        </div>
        <p>{{ item.body }}</p>
        <footer>
          <small>{{ formatDateTime(item.deliveredAt) }}</small>
          <button
            v-if="!item.read"
            type="button"
            @click="markRead(item.deliveryId)"
          >
            标记已读 Mark as read
          </button>
        </footer>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.breadcrumb {
  font-size: 0.85rem;
  color: var(--color-text-soft);
}

.page-grid {
  display: grid;
  gap: 12px;
}

.head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

h2,
h3 {
  margin: 0;
}

header p {
  margin: 4px 0 0;
  color: var(--color-text-soft);
}

.badge {
  border-radius: 999px;
  padding: 4px 10px;
  background: #e7f4ff;
  color: var(--color-primary);
}

.controls {
  padding: 8px;
  display: flex;
  gap: 8px;
}

.controls button,
.inbox-card button {
  min-height: 34px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  background: white;
  padding: 0 12px;
}

.card-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 10px;
}

.inbox-card {
  padding: 12px;
}

.inbox-card.unread {
  border-left: 4px solid var(--color-primary);
}

.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-row small {
  color: var(--color-text-soft);
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.dot.high {
  background: #d93f3f;
}

.dot.medium {
  background: #da9a2d;
}

.dot.low {
  background: #47a67d;
}

.inbox-card p {
  margin: 8px 0 10px;
  white-space: pre-wrap;
}

footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.empty {
  padding: 20px;
  text-align: center;
}

.illu {
  font-size: 2.4rem;
  margin: 0;
}
</style>
