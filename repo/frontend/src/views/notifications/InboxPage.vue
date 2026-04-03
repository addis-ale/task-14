<script setup lang="ts">
import { computed, ref } from "vue";
import { useNotifications } from "@/composables/useNotifications";
import { formatDateTime } from "@/utils/date";
import { api, unwrap } from "@/api";

const { inbox, unread, loading, markRead, fetchInbox } = useNotifications();

const cards = computed(() => inbox.value?.items || []);

const expandedDeliveryId = ref<number | null>(null);
const deliveryDetail = ref<Record<string, unknown> | null>(null);
const deliveryLoading = ref(false);

function priorityClass(priority: string): string {
  if (priority === "HIGH") return "high";
  if (priority === "MEDIUM") return "medium";
  return "low";
}

async function toggleDeliveryDetail(item: { deliveryId: number; notificationId: number }): Promise<void> {
  if (expandedDeliveryId.value === item.deliveryId) {
    expandedDeliveryId.value = null;
    deliveryDetail.value = null;
    return;
  }
  expandedDeliveryId.value = item.deliveryId;
  deliveryLoading.value = true;
  try {
    deliveryDetail.value = await unwrap(
      api.get(`/inbox/${item.deliveryId}/delivery-status`),
    );
  } catch {
    deliveryDetail.value = {
      channel: "-",
      status: "-",
      attempts: 0,
      lastAttemptAt: null,
      deliveredAt: null,
    };
  } finally {
    deliveryLoading.value = false;
  }
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
          <div class="footer-actions">
            <button
              v-if="!item.read"
              type="button"
              @click="markRead(item.deliveryId)"
            >
              标记已读 Mark as read
            </button>
            <button
              type="button"
              class="detail-btn"
              @click.stop="toggleDeliveryDetail(item)"
            >
              {{ expandedDeliveryId === item.deliveryId ? '收起 Hide' : '投递详情 Delivery' }}
            </button>
          </div>
        </footer>
        <!-- Delivery status detail -->
        <div
          v-if="expandedDeliveryId === item.deliveryId"
          class="delivery-detail"
        >
          <div v-if="deliveryLoading" class="detail-loading">加载中...</div>
          <div v-else-if="deliveryDetail" class="detail-grid">
            <div><strong>渠道 Channel</strong><span>{{ deliveryDetail.channel || '-' }}</span></div>
            <div><strong>状态 Status</strong><span>{{ deliveryDetail.status || '-' }}</span></div>
            <div><strong>尝试次数 Attempts</strong><span>{{ deliveryDetail.attempts ?? 0 }}</span></div>
            <div><strong>最后尝试 Last Attempt</strong><span>{{ formatDateTime(deliveryDetail.lastAttemptAt as string) }}</span></div>
            <div><strong>送达时间 Delivered</strong><span>{{ formatDateTime(deliveryDetail.deliveredAt as string) }}</span></div>
          </div>
        </div>
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

.footer-actions {
  display: flex;
  gap: 6px;
}

.detail-btn {
  font-size: 0.82rem;
  min-height: 30px;
  background: #f3f8fb;
}

.delivery-detail {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--color-border);
}

.detail-loading {
  color: var(--color-text-soft);
  font-size: 0.85rem;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 8px;
}

.detail-grid > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.detail-grid strong {
  font-size: 0.78rem;
  color: var(--color-text-soft);
}

.detail-grid span {
  font-size: 0.88rem;
}
</style>
