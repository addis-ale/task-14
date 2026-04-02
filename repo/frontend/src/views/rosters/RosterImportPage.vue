<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import ImportWizard from "@/components/ImportWizard/ImportWizard.vue";
import { api, unwrap } from "@/api";

const router = useRouter();
const committing = ref(false);
const message = ref("");

async function commit(
  rows: Array<{ values: Record<string, string> }>,
): Promise<void> {
  committing.value = true;
  message.value = "";
  try {
    await unwrap(
      api.post("/imports/rosters/commit", {
        entityType: "SESSION_CANDIDATE",
        rows: rows.map((row) => row.values),
      }),
    );
    message.value = `已提交 ${rows.length} 行 Successfully committed`;
  } catch {
    message.value = "导入接口暂不可用，已完成前端预校验。";
  } finally {
    committing.value = false;
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
    <p v-if="message" class="hint">{{ message }}</p>
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
</style>
