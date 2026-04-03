<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import ImportWizard from "@/components/ImportWizard/ImportWizard.vue";
import { api, unwrap } from "@/api";
import { handleApiError } from "@/utils/toast";

const router = useRouter();
const committing = ref(false);
const message = ref("");
const commitFailed = ref(false);
const lastCommitRows = ref<Array<{ values: Record<string, string> }>>([]);

async function commit(
  rows: Array<{ values: Record<string, string> }>,
): Promise<void> {
  committing.value = true;
  message.value = "";
  commitFailed.value = false;
  lastCommitRows.value = rows;
  try {
    await unwrap(
      api.post("/imports/rosters/commit", {
        entityType: "SESSION_CANDIDATE",
        rows: rows.map((row) => row.values),
      }),
    );
    message.value = `已提交 ${rows.length} 行 Successfully committed`;
    commitFailed.value = false;
  } catch (err) {
    commitFailed.value = true;
    message.value = "提交失败，请重试或联系管理员 Commit failed. Please retry or contact admin.";
    handleApiError(err);
  } finally {
    committing.value = false;
  }
}

function retryCommit(): void {
  if (lastCommitRows.value.length > 0) {
    void commit(lastCommitRows.value);
  }
}

function cancel(): void {
  void router.push("/rosters");
}
</script>

<template>
  <section class="page-grid">
    <header>
      <h2>名册批量导入</h2>
      <p>Upload -> Preview -> Commit workflow</p>
    </header>

    <ImportWizard
      :required-columns="['studentNo', 'studentName', 'seatNo']"
      @commit="commit"
      @cancel="cancel"
    />

    <p v-if="committing" class="hint">提交中 Committing...</p>
    <div v-if="message" :class="['result-msg', commitFailed ? 'error' : 'success']">
      <p>{{ message }}</p>
      <button v-if="commitFailed" type="button" class="retry-btn" @click="retryCommit">
        重试 Retry
      </button>
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

.hint {
  margin: 0;
  color: var(--color-text-soft);
}

.result-msg {
  padding: 12px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.result-msg p {
  margin: 0;
}

.result-msg.success {
  background: #e8f8ef;
  color: #2d8f57;
  border: 1px solid #b8e6cb;
}

.result-msg.error {
  background: #fdeeed;
  color: #9e3a35;
  border: 1px solid #f7c6c2;
}

.retry-btn {
  min-height: 34px;
  border-radius: 10px;
  border: 1px solid #f7c6c2;
  background: white;
  color: #9e3a35;
  padding: 0 14px;
  font: inherit;
  cursor: pointer;
}
</style>
