<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { useAutoSave } from "@/composables/useAutoSave";
import { formatDateTime } from "@/utils/date";

interface FormField {
  key: string;
  label: string;
  type: string;
  placeholder?: string;
  required?: boolean;
  options?: Array<{ label: string; value: string | number }>;
}

interface FormStep {
  title: string;
  subtitle: string;
  fields: FormField[];
}

const props = defineProps<{
  steps: FormStep[];
  formKey: string;
  modelValue: Record<string, any>;
  submitText?: string;
}>();

const emit = defineEmits<{
  "update:modelValue": [payload: Record<string, any>];
  submit: [payload: Record<string, any>];
}>();

const currentStep = ref(0);
const state = reactive<Record<string, any>>({ ...props.modelValue });
const errors = reactive<Record<string, string>>({});

const { saving, lastSavedAt, hasDraft, loadDraft, saveDraft } = useAutoSave({
  formKey: props.formKey,
  data: ref(state),
});

const activeStep = computed(() => props.steps[currentStep.value]);

watch(
  state,
  () => {
    emit("update:modelValue", { ...state });
  },
  { deep: true },
);

async function tryResumeDraft(): Promise<void> {
  const draft = await loadDraft();
  if (!draft) {
    return;
  }

  if (window.confirm("检测到草稿，是否恢复? Resume draft?")) {
    Object.assign(state, draft);
  }
}

function validateStep(): boolean {
  let valid = true;
  activeStep.value.fields.forEach((field) => {
    const value = state[field.key];
    if (
      field.required &&
      (value === null || value === undefined || value === "")
    ) {
      errors[field.key] = `${field.label} 为必填项`;
      valid = false;
    } else {
      errors[field.key] = "";
    }
  });
  return valid;
}

function next(): void {
  if (!validateStep()) {
    return;
  }
  currentStep.value = Math.min(currentStep.value + 1, props.steps.length - 1);
}

function prev(): void {
  currentStep.value = Math.max(currentStep.value - 1, 0);
}

async function submit(): Promise<void> {
  if (!validateStep()) {
    return;
  }
  await saveDraft();
  emit("submit", { ...state });
}

void tryResumeDraft();
</script>

<template>
  <section class="form-builder">
    <ol class="stepper" aria-label="Step indicator">
      <li
        v-for="(step, index) in steps"
        :key="step.title"
        :class="{ active: index === currentStep, done: index < currentStep }"
      >
        <span class="count">{{ index + 1 }}</span>
        <div>
          <strong>{{ step.title }}</strong>
          <small>{{ step.subtitle }}</small>
        </div>
      </li>
    </ol>

    <div class="step-card card">
      <h3>{{ activeStep.title }}</h3>
      <p>{{ activeStep.subtitle }}</p>

      <div class="fields-grid">
        <label
          v-for="field in activeStep.fields"
          :key="field.key"
          class="field-item"
        >
          <span>{{ field.label }}</span>

          <select v-if="field.type === 'select'" v-model="state[field.key]">
            <option value="">请选择 Select</option>
            <option
              v-for="item in field.options || []"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </option>
          </select>

          <textarea
            v-else-if="field.type === 'textarea'"
            v-model="state[field.key]"
            :placeholder="field.placeholder || ''"
          />

          <input
            v-else
            v-model="state[field.key]"
            :type="field.type"
            :placeholder="field.placeholder || ''"
          />

          <small class="error" v-if="errors[field.key]">{{
            errors[field.key]
          }}</small>
        </label>
      </div>
    </div>

    <footer class="actions">
      <div class="status">
        <small v-if="saving">自动保存中 Auto-saving...</small>
        <small v-else-if="hasDraft && lastSavedAt"
          >草稿已保存 {{ formatDateTime(lastSavedAt) }}</small
        >
      </div>
      <div class="btns">
        <button
          type="button"
          class="outline"
          :disabled="currentStep === 0"
          @click="prev"
        >
          上一步
        </button>
        <button
          type="button"
          class="outline"
          v-if="currentStep < steps.length - 1"
          @click="next"
        >
          下一步
        </button>
        <button type="button" class="primary" v-else @click="submit">
          {{ submitText || "提交 Submit" }}
        </button>
      </div>
    </footer>
  </section>
</template>

<style scoped>
.form-builder {
  display: grid;
  gap: 12px;
}

.stepper {
  list-style: none;
  display: grid;
  gap: 8px;
  padding: 0;
  margin: 0;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.stepper li {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 8px 10px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  background: #f8fbfd;
}

.stepper li.active {
  border-color: var(--color-primary);
  background: #e7f3fa;
}

.stepper li.done {
  border-color: #98d6c4;
}

.count {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--color-primary);
  color: white;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.86rem;
}

.step-card {
  padding: 14px;
}

.step-card h3 {
  margin: 0;
}

.step-card p {
  margin: 4px 0 12px;
  color: var(--color-text-soft);
}

.fields-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-item input,
.field-item select,
.field-item textarea {
  border: 1px solid var(--color-border);
  border-radius: 10px;
  min-height: 38px;
  padding: 8px 10px;
}

.field-item textarea {
  min-height: 92px;
}

.error {
  color: var(--color-danger);
}

.actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.btns {
  display: flex;
  gap: 8px;
}

.primary,
.outline {
  min-height: 38px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  padding: 0 14px;
}

.primary {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: white;
}

.outline {
  background: white;
}
</style>
