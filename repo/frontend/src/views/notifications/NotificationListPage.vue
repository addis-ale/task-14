<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import DataTable from "@/components/DataTable/DataTable.vue";
import { api, unwrap } from "@/api";
import { handleApiError, showSuccess } from "@/utils/toast";
import { formatDateTime } from "@/utils/date";
import { useI18n } from "@/i18n";
import { useRBAC } from "@/composables/useRBAC";
import type { PageData } from "@/types/api";
import type { TableColumn } from "@/types/ui";

const { can } = useRBAC();

const { t } = useI18n();
const router = useRouter();
const activeTab = ref("");
const filters = reactive({ eventType: "", status: "" });
const tableKey = ref(0);

const tabs = [
  { value: "", label: "全部 All" },
  { value: "DRAFT", label: t("notifications.draft") },
  { value: "PENDING_REVIEW", label: t("notifications.pendingReview") },
  { value: "APPROVED", label: t("notifications.approved") },
  { value: "SENDING", label: t("notifications.sending") },
  { value: "SENT", label: t("notifications.sent") },
  { value: "REJECTED", label: t("notifications.rejected") },
];

const columns: TableColumn[] = [
  { key: "id", label: "ID", sortable: true, width: "70px" },
  { key: "title", label: "标题 Title", sortable: true },
  { key: "eventType", label: "事件类型 Event", sortable: true },
  { key: "priority", label: "优先级 Priority", sortable: true },
  { key: "status", label: "状态 Status", sortable: true },
  { key: "complianceStatus", label: "合规 Compliance", sortable: true },
  { key: "publishedBy", label: "发布者 Publisher", sortable: false },
  { key: "createdAt", label: "创建时间 Created", sortable: true },
];

// Delivery detail state
const showDelivery = ref(false);
const deliveryNotifId = ref<number | null>(null);
const deliveries = ref<Record<string, unknown>[]>([]);
const deliveryLoading = ref(false);

async function fetcher(
  params: Record<string, unknown>,
): Promise<PageData<Record<string, unknown>>> {
  return unwrap(
    api.get("/notifications", { params: { ...params, ...filters } }),
  );
}

function switchTab(tab: string) {
  activeTab.value = tab;
  filters.status = tab;
  tableKey.value++;
}

function statusColor(status: unknown): string {
  switch (status) {
    case "DRAFT": return "badge-gray";
    case "PENDING_REVIEW": return "badge-yellow";
    case "APPROVED": return "badge-green";
    case "SENDING": return "badge-yellow";
    case "SENT": return "badge-green";
    case "REJECTED": return "badge-red";
    default: return "badge-gray";
  }
}

function priorityColor(priority: unknown): string {
  switch (priority) {
    case "HIGH": return "badge-red";
    case "MEDIUM": return "badge-yellow";
    case "LOW": return "badge-green";
    default: return "badge-gray";
  }
}

async function submitReview(row: Record<string, unknown>): Promise<void> {
  try {
    await unwrap(api.post(`/notifications/${row.id}/submit-review`));
    showSuccess(t("common.success"));
    tableKey.value++;
  } catch (err) {
    handleApiError(err);
  }
}

async function publish(row: Record<string, unknown>): Promise<void> {
  try {
    await unwrap(api.post(`/notifications/${row.id}/publish`));
    showSuccess(t("common.success"));
    tableKey.value++;
  } catch (err) {
    handleApiError(err);
  }
}

async function openDeliveryStatus(row: Record<string, unknown>): Promise<void> {
  deliveryNotifId.value = row.id as number;
  deliveryLoading.value = true;
  showDelivery.value = true;
  try {
    const data = await unwrap(api.get(`/notifications/${row.id}/deliveries`));
    deliveries.value = data.items || data || [];
  } catch (err) {
    handleApiError(err);
    deliveries.value = [];
  } finally {
    deliveryLoading.value = false;
  }
}

async function retryDelivery(delivery: Record<string, unknown>): Promise<void> {
  try {
    await unwrap(api.post(`/notifications/${deliveryNotifId.value}/deliveries/${delivery.id}/retry`));
    showSuccess(t("common.success"));
    if (deliveryNotifId.value) await openDeliveryStatus({ id: deliveryNotifId.value });
  } catch (err) {
    handleApiError(err);
  }
}
</script>

<template>
  <section class="page-grid">
    <nav class="breadcrumb" aria-label="Breadcrumb">
      <span>{{ t("nav.dashboard") }}</span> / <span>{{ t("nav.notifications") }}</span>
    </nav>

    <header class="head-row">
      <div>
        <h2>{{ t("notifications.title") }}</h2>
        <p>{{ t("notifications.subtitle") }}</p>
      </div>
      <button v-if="can('create')" type="button" class="primary-btn" @click="router.push('/notifications/create')">
        {{ t("notifications.create") }}
      </button>
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
      </button>
    </div>

    <DataTable
      :key="tableKey"
      :columns="columns"
      :fetcher="fetcher"
      :filters="filters"
    >
      <template #cell-status="{ row }">
        <span class="status-badge" :class="statusColor(row.status)">{{ row.status }}</span>
      </template>
      <template #cell-priority="{ row }">
        <span class="status-badge" :class="priorityColor(row.priority)">{{ row.priority }}</span>
      </template>
      <template #cell-complianceStatus="{ row }">
        <span class="status-badge" :class="statusColor(row.complianceStatus)">{{ row.complianceStatus || '-' }}</span>
      </template>
      <template #cell-createdAt="{ row }">
        {{ formatDateTime(row.createdAt as string) }}
      </template>
      <template #actions="{ row }">
        <div class="row-actions">
          <button
            v-if="row.status === 'DRAFT' && can('review')"
            type="button"
            @click.stop="submitReview(row)"
          >{{ t("notifications.submitForReview") }}</button>
          <button
            v-if="row.status === 'APPROVED' && can('publish')"
            type="button"
            class="publish-btn"
            @click.stop="publish(row)"
          >{{ t("common.publish") }}</button>
          <button
            v-if="row.status === 'SENT' || row.status === 'SENDING'"
            type="button"
            @click.stop="openDeliveryStatus(row)"
          >{{ t("notifications.deliveryStatus") }}</button>
        </div>
      </template>
    </DataTable>

    <!-- Delivery Status Modal -->
    <div v-if="showDelivery" class="modal-backdrop" role="dialog" aria-modal="true" @click.self="showDelivery = false">
      <div class="modal card">
        <header class="modal-header">
          <h3>{{ t("notifications.deliveryStatus") }} #{{ deliveryNotifId }}</h3>
          <button type="button" class="outline-btn" @click="showDelivery = false">{{ t("common.close") }}</button>
        </header>

        <div v-if="deliveryLoading" class="loading">{{ t("common.loading") }}</div>

        <div v-else-if="deliveries.length === 0" class="empty-state">
          <p>{{ t("common.noData") }}</p>
        </div>

        <table v-else class="delivery-table">
          <thead>
            <tr>
              <th>{{ t("notifications.recipient") }}</th>
              <th>{{ t("notifications.channel") }}</th>
              <th>{{ t("common.status") }}</th>
              <th>{{ t("notifications.deliveryAttempts") }}</th>
              <th>{{ t("common.actions") }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="d in deliveries" :key="String(d.id)">
              <td>{{ d.recipientName || d.recipientId || '-' }}</td>
              <td>{{ d.channel || '-' }}</td>
              <td>
                <span class="status-badge" :class="d.status === 'DELIVERED' ? 'badge-green' : d.status === 'FAILED' ? 'badge-red' : 'badge-yellow'">
                  {{ d.status }}
                </span>
              </td>
              <td>{{ d.attempts || 0 }}</td>
              <td>
                <button
                  v-if="d.status === 'FAILED'"
                  type="button"
                  class="retry-btn"
                  @click="retryDelivery(d)"
                >{{ t("notifications.retryDelivery") }}</button>
              </td>
            </tr>
          </tbody>
        </table>
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

.head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

h2, h3 {
  margin: 0;
}

p {
  margin: 5px 0 0;
  color: var(--color-text-soft);
}

.tab-bar {
  display: flex;
  gap: 2px;
  border-bottom: 2px solid var(--color-border);
  overflow-x: auto;
}

.tab-btn {
  min-height: 38px;
  border: none;
  border-bottom: 2px solid transparent;
  background: none;
  padding: 0 12px;
  cursor: pointer;
  font: inherit;
  color: var(--color-text-soft);
  margin-bottom: -2px;
  white-space: nowrap;
}

.tab-btn.active {
  border-bottom-color: var(--color-primary);
  color: var(--color-primary);
  font-weight: 600;
}

.row-actions {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

button {
  min-height: 36px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  padding: 0 10px;
  font: inherit;
  cursor: pointer;
}

.primary-btn {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
}

.publish-btn {
  background: var(--color-success);
  border-color: var(--color-success);
  color: white;
}

.retry-btn {
  background: #fdeeed;
  border-color: #f7c6c2;
  color: #9e3a35;
  font-size: 0.82rem;
  min-height: 30px;
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

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(25, 40, 50, 0.32);
  display: grid;
  place-items: center;
  z-index: 20;
}

.modal {
  width: min(700px, calc(100vw - 28px));
  padding: 18px;
  max-height: 90vh;
  overflow: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.outline-btn {
  background: white;
}

.loading, .empty-state {
  padding: 20px;
  text-align: center;
  color: var(--color-text-soft);
}

.delivery-table {
  width: 100%;
  border-collapse: collapse;
}

.delivery-table th,
.delivery-table td {
  text-align: left;
  padding: 8px 10px;
  border-bottom: 1px solid var(--color-border);
}

.delivery-table thead {
  background: var(--color-surface-alt);
}
</style>
