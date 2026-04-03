<script setup lang="ts">
import { reactive, ref, computed } from "vue";
import { useRouter } from "vue-router";
import FormBuilder from "@/components/FormBuilder/FormBuilder.vue";
import { api, unwrap } from "@/api";
import { handleApiError, showSuccess } from "@/utils/toast";
import { useI18n } from "@/i18n";
import { useRBAC } from "@/composables/useRBAC";

const { t } = useI18n();
const { can } = useRBAC();
const router = useRouter();

const model = reactive<Record<string, unknown>>({
  eventType: "",
  title: "",
  body: "",
  priority: "HIGH",
  gradeId: "",
  classId: "",
});

const error = ref("");
const showPreview = ref(false);
const createdNotifId = ref<number | null>(null);
const notifStatus = ref<string>(""); // DRAFT, PENDING_REVIEW, etc.

const steps = [
  {
    title: "步骤1 通知基本信息",
    subtitle: "Event type and basic metadata",
    fields: [
      {
        key: "eventType",
        label: "事件类型 Event",
        type: "select",
        required: true,
        options: [
          { label: "EXAM_SCHEDULE_PUBLISHED", value: "EXAM_SCHEDULE_PUBLISHED" },
          { label: "EXAM_TIME_CHANGED", value: "EXAM_TIME_CHANGED" },
          { label: "EXAM_ROOM_CHANGED", value: "EXAM_ROOM_CHANGED" },
        ],
      },
      {
        key: "priority",
        label: "优先级 Priority",
        type: "select",
        required: true,
        options: [
          { label: "HIGH", value: "HIGH" },
          { label: "MEDIUM", value: "MEDIUM" },
          { label: "LOW", value: "LOW" },
        ],
      },
    ],
  },
  {
    title: "步骤2 通知内容",
    subtitle: "Compose title and body content",
    fields: [
      { key: "title", label: "标题 Title", type: "text", required: true },
      { key: "body", label: "正文 Body", type: "textarea", required: true },
    ],
  },
  {
    title: "步骤3 目标范围",
    subtitle: "Set target classes / grades",
    fields: [
      { key: "gradeId", label: "年级ID Grade", type: "number", required: true },
      { key: "classId", label: "班级ID Class", type: "number", required: true },
    ],
  },
];

const priorityClass = computed(() => {
  switch (model.priority) {
    case "HIGH": return "preview-high";
    case "MEDIUM": return "preview-medium";
    case "LOW": return "preview-low";
    default: return "";
  }
});

function openPreview(payload: Record<string, unknown>) {
  Object.assign(model, payload);
  showPreview.value = true;
}

async function createDraft(): Promise<void> {
  if (!can("create")) {
    return;
  }
  error.value = "";
  try {
    const result = await unwrap(
      api.post("/notifications", {
        eventType: model.eventType,
        title: model.title,
        body: model.body,
        priority: model.priority,
        targetScope: {
          gradeIds: [Number(model.gradeId)],
          classIds: [Number(model.classId)],
        },
      }),
    );
    createdNotifId.value = result.id || result;
    notifStatus.value = "DRAFT";
    showPreview.value = false;
    showSuccess(t("common.success"));
  } catch (err) {
    handleApiError(err);
    error.value = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || t("common.error");
  }
}

async function submitForReview(): Promise<void> {
  if (!createdNotifId.value || !can("review")) return;
  try {
    await unwrap(api.post(`/notifications/${createdNotifId.value}/submit-review`));
    notifStatus.value = "PENDING_REVIEW";
    showSuccess(t("common.success"));
  } catch (err) {
    handleApiError(err);
  }
}

async function publishNotification(): Promise<void> {
  if (!createdNotifId.value || !can("publish")) return;
  try {
    await unwrap(api.post(`/notifications/${createdNotifId.value}/publish`));
    showSuccess(t("common.success"));
    await router.push("/notifications");
  } catch (err) {
    handleApiError(err);
  }
}
</script>

<template>
  <section class="page-grid">
    <nav class="breadcrumb" aria-label="Breadcrumb">
      <span>{{ t("nav.dashboard") }}</span> /
      <RouterLink to="/notifications">{{ t("nav.notifications") }}</RouterLink> /
      <span>{{ t("notifications.createFlow") }}</span>
    </nav>

    <header>
      <h2>{{ t("notifications.createFlow") }}</h2>
      <p>Create notification with compliance-friendly structure</p>
    </header>

    <!-- If already created, show workflow buttons -->
    <div v-if="createdNotifId" class="workflow-panel card">
      <div class="workflow-status">
        <span class="step-dot" :class="{ done: true }">1</span>
        <span class="step-label done-label">{{ t("notifications.draft") }}</span>
        <span class="step-line" :class="{ done: notifStatus !== 'DRAFT' }" />
        <span class="step-dot" :class="{ done: notifStatus === 'PENDING_REVIEW' || notifStatus === 'APPROVED' }">2</span>
        <span class="step-label" :class="{ 'done-label': notifStatus !== 'DRAFT' }">{{ t("notifications.pendingReview") }}</span>
        <span class="step-line" :class="{ done: notifStatus === 'APPROVED' }" />
        <span class="step-dot" :class="{ done: notifStatus === 'APPROVED' }">3</span>
        <span class="step-label" :class="{ 'done-label': notifStatus === 'APPROVED' }">{{ t("common.publish") }}</span>
      </div>
      <div class="workflow-actions">
        <button
          v-if="notifStatus === 'DRAFT' && can('review')"
          type="button"
          class="primary-btn"
          @click="submitForReview"
        >{{ t("notifications.submitForReview") }}</button>
        <button
          v-if="notifStatus === 'APPROVED' && can('publish')"
          type="button"
          class="publish-btn"
          @click="publishNotification"
        >{{ t("common.publish") }}</button>
        <button type="button" class="outline-btn" @click="router.push('/notifications')">
          {{ t("common.back") }}
        </button>
      </div>
    </div>

    <!-- Form builder (hidden after creation) -->
    <template v-if="!createdNotifId">
      <FormBuilder
        form-key="notification-create-form"
        :steps="steps"
        :model-value="model"
        submit-text="预览 Preview"
        @update:model-value="Object.assign(model, $event)"
        @submit="openPreview"
      />

      <p v-if="error" class="error-text">{{ error }}</p>
    </template>

    <!-- Preview Modal -->
    <div v-if="showPreview" class="modal-backdrop" role="dialog" aria-modal="true">
      <div class="modal card">
        <header class="modal-header">
          <h3>{{ t("notifications.previewStep") }}</h3>
          <button type="button" class="outline-btn" @click="showPreview = false">{{ t("common.close") }}</button>
        </header>

        <div class="preview-content" :class="priorityClass">
          <div class="preview-meta">
            <span class="preview-tag">{{ model.eventType }}</span>
            <span class="preview-tag">{{ model.priority }}</span>
          </div>
          <h4>{{ model.title }}</h4>
          <p class="preview-body">{{ model.body }}</p>
          <div class="preview-scope">
            <small>年级 Grade: {{ model.gradeId }} | 班级 Class: {{ model.classId }}</small>
          </div>
        </div>

        <div class="modal-actions">
          <button v-if="can('create')" type="button" class="primary-btn" @click="createDraft">
            {{ t("common.create") }} ({{ t("notifications.draft") }})
          </button>
          <button type="button" class="outline-btn" @click="showPreview = false">
            {{ t("common.back") }}
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

.breadcrumb a {
  color: var(--color-primary);
  text-decoration: none;
}

h2, h3, h4 {
  margin: 0;
}

p {
  margin: 4px 0 0;
  color: var(--color-text-soft);
}

.error-text {
  color: var(--color-danger);
  margin: 0;
}

/* Workflow panel */
.workflow-panel {
  padding: 16px;
  display: grid;
  gap: 14px;
}

.workflow-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid var(--color-border);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text-soft);
}

.step-dot.done {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
}

.step-label {
  font-size: 0.85rem;
  color: var(--color-text-soft);
}

.done-label {
  color: var(--color-primary);
  font-weight: 500;
}

.step-line {
  flex: 1;
  height: 2px;
  background: var(--color-border);
  max-width: 60px;
}

.step-line.done {
  background: var(--color-primary);
}

.workflow-actions {
  display: flex;
  gap: 8px;
}

button {
  min-height: 38px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  padding: 0 14px;
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

.outline-btn {
  background: white;
}

/* Modal */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(25, 40, 50, 0.32);
  display: grid;
  place-items: center;
  z-index: 20;
}

.modal {
  width: min(560px, calc(100vw - 28px));
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

.modal-actions {
  display: flex;
  gap: 8px;
  margin-top: 14px;
}

/* Preview */
.preview-content {
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 14px;
  border-left: 4px solid var(--color-primary);
}

.preview-high { border-left-color: var(--color-danger); }
.preview-medium { border-left-color: var(--color-warning); }
.preview-low { border-left-color: var(--color-success); }

.preview-meta {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}

.preview-tag {
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: 6px;
  background: #e7f3fa;
  color: var(--color-primary);
}

.preview-content h4 {
  margin-bottom: 6px;
  color: var(--color-text);
}

.preview-body {
  white-space: pre-wrap;
  color: var(--color-text);
}

.preview-scope {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid var(--color-border);
}
</style>
