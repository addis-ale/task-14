<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import FormBuilder from "@/components/FormBuilder/FormBuilder.vue";
import { api, unwrap } from "@/api";

const router = useRouter();
const formModel = reactive<Record<string, unknown>>({
  termId: "",
  gradeId: "",
  subjectId: "",
  examDate: "",
  startTime: "",
  endTime: "",
  roomIds: "",
  candidates: "",
  proctors: "",
});

const submitting = ref(false);
const error = ref("");

const steps = [
  {
    title: "步骤1 选择学期年级科目",
    subtitle: "Select term, grade and subject",
    fields: [
      { key: "termId", label: "学期ID", type: "number", required: true },
      { key: "gradeId", label: "年级ID", type: "number", required: true },
      { key: "subjectId", label: "科目ID", type: "number", required: true },
    ],
  },
  {
    title: "步骤2 设置考试时间",
    subtitle: "Set date and start/end time",
    fields: [
      { key: "examDate", label: "考试日期", type: "date", required: true },
      { key: "startTime", label: "开始时间", type: "time", required: true },
      { key: "endTime", label: "结束时间", type: "time", required: true },
    ],
  },
  {
    title: "步骤3 选择教室",
    subtitle: "Select rooms and check capacity",
    fields: [
      {
        key: "roomIds",
        label: "教室ID列表(逗号分隔)",
        type: "text",
        required: true,
      },
    ],
  },
  {
    title: "步骤4 分配考生",
    subtitle: "Assign candidates with seat constraints",
    fields: [
      {
        key: "candidates",
        label: "考生ID列表(逗号分隔)",
        type: "textarea",
        required: true,
      },
    ],
  },
  {
    title: "步骤5 分配监考老师",
    subtitle: "Assign proctors and inspect conflicts",
    fields: [
      {
        key: "proctors",
        label: "监考老师ID列表(逗号分隔)",
        type: "textarea",
        required: true,
      },
    ],
  },
  {
    title: "步骤6 审核与提交",
    subtitle: "Review and submit after conflict check",
    fields: [],
  },
];

function parseIds(raw: unknown): number[] {
  return String(raw || "")
    .split(",")
    .map((value) => Number(value.trim()))
    .filter((value) => Number.isFinite(value) && value > 0);
}

async function submit(payload: Record<string, unknown>): Promise<void> {
  submitting.value = true;
  error.value = "";

  const request = {
    termId: Number(payload.termId),
    gradeId: Number(payload.gradeId),
    subjectId: Number(payload.subjectId),
    examDate: payload.examDate,
    startTime: payload.startTime,
    endTime: payload.endTime,
    roomIds: parseIds(payload.roomIds),
    candidateIds: parseIds(payload.candidates),
    proctorIds: parseIds(payload.proctors),
  };

  try {
    await unwrap(api.post("/sessions/conflict-check", request));
    const saved = await unwrap(api.post("/sessions", request));
    await router.push(`/scheduling/sessions/${saved.id}`);
  } catch (err) {
    error.value =
      (err as { response?: { data?: { message?: string } } })?.response?.data
        ?.message || "保存失败，请检查冲突报告";
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <section class="page-grid">
    <header>
      <h2>新建/编辑考试场次</h2>
      <p>6-step scheduling wizard with auto-save draft every 30 seconds</p>
    </header>

    <FormBuilder
      form-key="session-create-form"
      :steps="steps"
      :model-value="formModel"
      submit-text="提交场次 Submit Session"
      @update:model-value="Object.assign(formModel, $event)"
      @submit="submit"
    />

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="submitting" class="hint">提交中 Submitting...</p>
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

header p,
.hint {
  color: var(--color-text-soft);
  margin: 4px 0 0;
}

.error {
  color: var(--color-danger);
  margin: 0;
}
</style>
