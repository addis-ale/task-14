<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuth } from "@/composables/useAuth";
import { useAuthStore } from "@/stores/auth";
import { logger } from "@/utils/logger";

const route = useRoute();
const router = useRouter();
const auth = useAuth();
const authStore = useAuthStore();

const username = ref("");
const password = ref("");
const remember = ref(false);
const loading = ref(false);
const errorMessage = ref("");
const lockSeconds = ref(0);

const passwordStrength = computed(() => {
  const p = password.value;
  if (!p) return { level: 0, label: "", cls: "" };

  let score = 0;
  if (p.length >= 12) score++;
  if (/[A-Z]/.test(p)) score++;
  if (/[a-z]/.test(p)) score++;
  if (/[0-9]/.test(p)) score++;
  if (/[^A-Za-z0-9]/.test(p)) score++;

  if (score <= 2) return { level: 1, label: "弱 Weak", cls: "weak" };
  if (score <= 3) return { level: 2, label: "中 Medium", cls: "medium" };
  return { level: 3, label: "强 Strong", cls: "strong" };
});

async function submit(): Promise<void> {
  loading.value = true;
  errorMessage.value = "";

  try {
    await auth.login(username.value, password.value, remember.value);

    // Validate redirect to prevent open-redirect attacks
    const rawRedirect = route.query.redirect as string | undefined;
    const safePath = authStore.validateRedirect(rawRedirect);
    await router.push(safePath);
  } catch (error) {
    const message = (error as { response?: { data?: { message?: string } } })
      ?.response?.data?.message;
    errorMessage.value = message || "登录失败，请检查用户名和密码";
    logger.warn("Login", "Login failed");
    if (errorMessage.value.toLowerCase().includes("locked")) {
      lockSeconds.value = 300;
      const timer = window.setInterval(() => {
        lockSeconds.value = Math.max(0, lockSeconds.value - 1);
        if (lockSeconds.value === 0) {
          window.clearInterval(timer);
        }
      }, 1000);
    }
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-card card" aria-label="Login form">
      <header>
        <h1>安全排考与通知系统</h1>
        <p>Secure Exam Scheduling & Notification Management</p>
      </header>

      <form class="form" @submit.prevent="submit">
        <label>
          用户名 Username
          <input
            v-model="username"
            type="text"
            autocomplete="username"
            required
          />
        </label>

        <label>
          密码 Password
          <input
            v-model="password"
            type="password"
            autocomplete="current-password"
            required
          />
          <div v-if="password" class="strength-meter">
            <div class="strength-bar" :class="passwordStrength.cls">
              <div class="strength-fill" :style="{ width: (passwordStrength.level / 3) * 100 + '%' }" />
            </div>
            <small>密码强度 Strength: {{ passwordStrength.label }}</small>
          </div>
        </label>

        <label class="remember">
          <input v-model="remember" type="checkbox" />
          记住此设备 Remember this device
        </label>

        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
        <p v-if="lockSeconds > 0" class="warning">
          账号锁定倒计时 Unlock timer: {{ lockSeconds }}s
        </p>

        <button type="submit" :disabled="loading">
          {{ loading ? "登录中..." : "登录 Sign In" }}
        </button>
      </form>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 16px;
}

.login-card {
  width: min(460px, 100%);
  padding: 22px;
  background: linear-gradient(
    160deg,
    rgba(255, 255, 255, 0.96),
    rgba(240, 248, 253, 0.92)
  );
}

header h1 {
  margin: 0;
  font-family: var(--font-heading);
  font-size: 1.4rem;
}

header p {
  margin: 6px 0 16px;
  color: var(--color-text-soft);
}

.form {
  display: grid;
  gap: 12px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

input[type="text"],
input[type="password"] {
  border: 1px solid var(--color-border);
  min-height: 40px;
  border-radius: 10px;
  padding: 0 12px;
}

.remember {
  flex-direction: row;
  align-items: center;
}

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

button {
  min-height: 40px;
  border-radius: 10px;
  border: 1px solid var(--color-primary);
  background: var(--color-primary);
  color: white;
  font-weight: 600;
  cursor: pointer;
}

.error {
  color: var(--color-danger);
  margin: 0;
}

.warning {
  color: var(--color-warning);
  margin: 0;
}
</style>
