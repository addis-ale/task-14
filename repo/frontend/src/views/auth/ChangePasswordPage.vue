<script setup lang="ts">
import { reactive, ref } from "vue";
import { api, unwrap } from "@/api";

const form = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
});

const message = ref("");
const error = ref("");
const loading = ref(false);

async function submit(): Promise<void> {
  message.value = "";
  error.value = "";
  if (form.newPassword !== form.confirmPassword) {
    error.value = "两次输入密码不一致 Password confirmation mismatch";
    return;
  }

  loading.value = true;
  try {
    await unwrap(
      api.put("/auth/password", {
        oldPassword: form.oldPassword,
        newPassword: form.newPassword,
      }),
    );
    message.value = "密码已更新 Password changed successfully";
    form.oldPassword = "";
    form.newPassword = "";
    form.confirmPassword = "";
  } catch (err) {
    error.value =
      (err as { response?: { data?: { message?: string } } })?.response?.data
        ?.message || "修改失败";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <section class="card form-shell">
    <h2>修改密码</h2>
    <p>Change account password</p>

    <form @submit.prevent="submit">
      <label>
        当前密码 Current password
        <input v-model="form.oldPassword" type="password" required />
      </label>
      <label>
        新密码 New password
        <input v-model="form.newPassword" type="password" required />
      </label>
      <label>
        确认密码 Confirm password
        <input v-model="form.confirmPassword" type="password" required />
      </label>

      <p class="ok" v-if="message">{{ message }}</p>
      <p class="error" v-if="error">{{ error }}</p>

      <button type="submit" :disabled="loading">
        {{ loading ? "提交中..." : "保存 Save" }}
      </button>
    </form>
  </section>
</template>

<style scoped>
.form-shell {
  max-width: 600px;
  padding: 16px;
}

h2 {
  margin: 0;
}

p {
  color: var(--color-text-soft);
}

form {
  display: grid;
  gap: 12px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

input,
button {
  min-height: 40px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  padding: 0 12px;
}

button {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: white;
}

.error {
  color: var(--color-danger);
  margin: 0;
}

.ok {
  color: var(--color-success);
  margin: 0;
}
</style>
