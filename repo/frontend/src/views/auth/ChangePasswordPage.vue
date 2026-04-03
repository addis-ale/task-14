<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { api, unwrap } from "@/api";
import { handleApiError, showSuccess } from "@/utils/toast";

const form = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
});

const message = ref("");
const error = ref("");
const loading = ref(false);

const passwordStrength = computed(() => {
  const p = form.newPassword;
  if (!p) return { level: 0, label: "", cls: "", valid: false };
  let score = 0;
  if (p.length >= 12) score++;
  if (/[A-Z]/.test(p)) score++;
  if (/[a-z]/.test(p)) score++;
  if (/[0-9]/.test(p)) score++;
  if (/[^A-Za-z0-9]/.test(p)) score++;
  const valid = score === 5;
  if (score <= 2) return { level: 1, label: "弱 Weak", cls: "weak", valid };
  if (score <= 3) return { level: 2, label: "中 Medium", cls: "medium", valid };
  return { level: 3, label: "强 Strong", cls: "strong", valid };
});

const canSubmit = computed(() => {
  return (
    form.oldPassword.length > 0 &&
    form.newPassword.length > 0 &&
    form.confirmPassword.length > 0 &&
    form.newPassword === form.confirmPassword &&
    passwordStrength.value.valid &&
    !loading.value
  );
});

async function submit(): Promise<void> {
  message.value = "";
  error.value = "";

  if (form.newPassword !== form.confirmPassword) {
    error.value = "两次输入密码不一致 Password confirmation mismatch";
    return;
  }

  if (!passwordStrength.value.valid) {
    error.value = "密码不符合策略 Password does not meet policy: >=12 chars, upper, lower, digit, special";
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
    showSuccess("密码已更新 Password changed successfully");
    message.value = "密码已更新 Password changed successfully";
    form.oldPassword = "";
    form.newPassword = "";
    form.confirmPassword = "";
  } catch (err) {
    handleApiError(err);
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
        <div v-if="form.newPassword" class="strength-meter">
          <div class="strength-bar" :class="passwordStrength.cls">
            <div class="strength-fill" :style="{ width: (passwordStrength.level / 3) * 100 + '%' }" />
          </div>
          <small>密码强度 Strength: {{ passwordStrength.label }}</small>
        </div>
        <small v-if="form.newPassword && !passwordStrength.valid" class="policy-hint">
          需要 >=12 字符，包含大写、小写、数字、特殊字符 / >=12 chars, upper, lower, digit, special
        </small>
      </label>
      <label>
        确认密码 Confirm password
        <input v-model="form.confirmPassword" type="password" required />
        <small v-if="form.confirmPassword && form.newPassword !== form.confirmPassword" class="error-hint">
          密码不一致 Passwords do not match
        </small>
      </label>

      <p class="ok" v-if="message">{{ message }}</p>
      <p class="error" v-if="error">{{ error }}</p>

      <button type="submit" :disabled="!canSubmit">
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

h2 { margin: 0; }

p { color: var(--color-text-soft); }

form {
  display: grid;
  gap: 12px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

input, button {
  min-height: 40px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  padding: 0 12px;
}

button {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: white;
  cursor: pointer;
}

button:disabled { opacity: 0.5; cursor: not-allowed; }

.strength-meter {
  display: flex;
  align-items: center;
  gap: 8px;
}

.strength-bar {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  background: #e5e7eb;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s;
}

.strength-bar.weak .strength-fill { background: var(--color-danger); }
.strength-bar.medium .strength-fill { background: var(--color-warning); }
.strength-bar.strong .strength-fill { background: var(--color-success); }

.policy-hint { color: var(--color-warning); font-size: 0.78rem; }
.error-hint { color: var(--color-danger); font-size: 0.78rem; }
.error { color: var(--color-danger); margin: 0; }
.ok { color: var(--color-success); margin: 0; }
</style>
