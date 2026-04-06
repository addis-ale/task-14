<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { RouterLink, RouterView, useRoute, useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { useAuth } from "@/composables/useAuth";
import { useAuthStore } from "@/stores/auth";
import { useI18n } from "@/i18n";
import { toasts } from "@/utils/toast";
import { api, unwrap } from "@/api";
import type { RoleName } from "@/types/ui";
import type { Locale } from "@/i18n";

interface MenuItem {
  key: string;
  to: string;
  zh: string;
  en: string;
  badgeKey?: string;
}

const route = useRoute();
const router = useRouter();
const auth = useAuth();
const authStore = useAuthStore();
const { user, activeRole, sessionWarningVisible, concurrentSessionDetected, activeSessions } = storeToRefs(authStore);
const { locale, setLocale } = useI18n();

// Badge counts
const unreadCount = ref(0);
const pendingReviewCount = ref(0);
const pendingAntiCheatCount = ref(0);
let badgeTimer: number | undefined;

const roleMenus: Record<RoleName, MenuItem[]> = {
  ADMIN: [
    { key: "dashboard", to: "/dashboard", zh: "仪表盘", en: "Dashboard" },
    { key: "users", to: "/admin/users", zh: "用户管理", en: "User Management" },
    { key: "sessions", to: "/scheduling/sessions", zh: "排考管理", en: "Scheduling" },
    { key: "rosters", to: "/rosters", zh: "名册管理", en: "Rosters" },
    { key: "campus", to: "/admin/campus-rooms", zh: "校区教室", en: "Campus & Rooms" },
    { key: "notice", to: "/notifications", zh: "通知管理", en: "Notifications" },
    { key: "compliance", to: "/notifications/compliance-reviews", zh: "合规审核", en: "Compliance Reviews", badgeKey: "review" },
    { key: "anticheat", to: "/notifications/anti-cheat", zh: "反作弊审核", en: "Anti-Cheat Review", badgeKey: "anticheat" },
    { key: "jobs", to: "/admin/jobs", zh: "任务监控", en: "Job Monitor" },
    { key: "audit", to: "/admin/audit-logs", zh: "审计日志", en: "Audit Logs" },
  ],
  ACADEMIC_AFFAIRS: [
    { key: "dashboard", to: "/dashboard", zh: "仪表盘", en: "Dashboard" },
    { key: "sessions", to: "/scheduling/sessions", zh: "排考管理", en: "Scheduling" },
    { key: "rosters", to: "/rosters", zh: "名册管理", en: "Rosters" },
    { key: "campus", to: "/admin/campus-rooms", zh: "校区教室", en: "Campus & Rooms" },
    { key: "notice", to: "/notifications", zh: "通知管理", en: "Notifications" },
    { key: "compliance", to: "/notifications/compliance-reviews", zh: "合规审核", en: "Compliance Reviews", badgeKey: "review" },
    { key: "anticheat", to: "/notifications/anti-cheat", zh: "反作弊审核", en: "Anti-Cheat Review", badgeKey: "anticheat" },
    { key: "audit", to: "/admin/audit-logs", zh: "审计日志", en: "Audit Logs" },
  ],
  HOMEROOM_TEACHER: [
    { key: "dashboard", to: "/dashboard", zh: "仪表盘", en: "Dashboard" },
    { key: "sessions", to: "/scheduling/sessions", zh: "排考查看", en: "Scheduling Readonly" },
    { key: "rosters", to: "/rosters", zh: "名册查看", en: "Rosters" },
  ],
  SUBJECT_TEACHER: [
    { key: "dashboard", to: "/dashboard", zh: "仪表盘", en: "Dashboard" },
    { key: "sessions", to: "/scheduling/sessions", zh: "排考查看", en: "Scheduling Readonly" },
    { key: "rosters", to: "/rosters", zh: "课程名册", en: "Course Rosters" },
  ],
  STUDENT: [
    { key: "exam", to: "/student/exams", zh: "我的考试", en: "My Exams" },
    { key: "inbox", to: "/notifications/inbox", zh: "消息中心", en: "Inbox", badgeKey: "unread" },
    { key: "pref", to: "/student/preferences", zh: "通知偏好", en: "Notification Preferences" },
  ],
};

const sidebarItems = computed(
  () => roleMenus[(activeRole.value || "") as RoleName] || [],
);
const draftDialogVisible = ref(false);
const draftedForms = ref<Array<{ formKey: string; updatedAt: string }>>([]);
const sidebarCollapsed = ref(false);

function getBadgeCount(key?: string): number {
  if (!key) return 0;
  if (key === "unread") return unreadCount.value;
  if (key === "review") return pendingReviewCount.value;
  if (key === "anticheat") return pendingAntiCheatCount.value;
  return 0;
}

async function fetchBadges() {
  if (!authStore.isAuthenticated) return;
  try {
    if (activeRole.value === "STUDENT") {
      const data = await unwrap(api.get("/inbox/unread-count"));
      unreadCount.value = data.unreadCount || 0;
    } else {
      const data = await unwrap(api.get("/dashboard/stats"));
      pendingReviewCount.value = data.pendingReviews || 0;
      pendingAntiCheatCount.value = data.pendingAntiCheatFlags || 0;
    }
  } catch {
    // silent
  }
  // Periodically check for concurrent sessions
  void authStore.checkConcurrentSessions();
}

function forceLogoutOthers(): void {
  void authStore.forceLogoutOtherSessions();
}

function dismissConcurrentWarning(): void {
  authStore.dismissConcurrentWarning();
}

async function onSwitchRole(event: Event): Promise<void> {
  const select = event.target as HTMLSelectElement;
  await auth.switchRole(select.value);
  await router.push(
    authStore.activeRole === "STUDENT" ? "/student/exams" : "/dashboard",
  );
}

async function onLogout(): Promise<void> {
  await auth.logout();
}

function resumeDraft(): void {
  draftDialogVisible.value = false;
  if (draftedForms.value[0]?.formKey.includes("session")) {
    void router.push("/scheduling/sessions/new");
    return;
  }
  void router.push("/rosters/import");
}

function toggleLocale() {
  setLocale(locale.value === "zh-CN" ? "en" : "zh-CN");
}

function onVisibilityChange() {
  if (document.hidden) {
    if (badgeTimer) {
      window.clearInterval(badgeTimer);
      badgeTimer = undefined;
    }
  } else {
    void fetchBadges();
    badgeTimer = window.setInterval(fetchBadges, 60_000);
  }
}

onMounted(() => {
  const drafts = auth.consumeDraftPrompt();
  if (drafts.length > 0) {
    draftedForms.value = drafts;
    draftDialogVisible.value = true;
  }
  void fetchBadges();
  badgeTimer = window.setInterval(fetchBadges, 60_000);
  document.addEventListener("visibilitychange", onVisibilityChange);
});

onBeforeUnmount(() => {
  if (badgeTimer) window.clearInterval(badgeTimer);
  document.removeEventListener("visibilitychange", onVisibilityChange);
});
</script>

<template>
  <div class="layout-shell" :class="{ collapsed: sidebarCollapsed }">
    <aside class="sidebar card" aria-label="Main navigation">
      <div class="brand">
        <h1>安全排考系统</h1>
        <p>Secure Exam Scheduler</p>
      </div>
      <button
        type="button"
        class="collapse-btn"
        :aria-label="sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'"
        @click="sidebarCollapsed = !sidebarCollapsed"
      >
        {{ sidebarCollapsed ? '▸' : '◂' }}
      </button>
      <nav>
        <RouterLink
          v-for="item in sidebarItems"
          :key="item.key"
          :to="item.to"
          class="nav-item"
          :class="{ active: route.path.startsWith(item.to) }"
        >
          <span>{{ item.zh }}</span>
          <small>{{ item.en }}</small>
          <span
            v-if="getBadgeCount(item.badgeKey) > 0"
            class="nav-badge"
          >{{ getBadgeCount(item.badgeKey) }}</span>
        </RouterLink>
      </nav>
    </aside>

    <main class="content-area">
      <header class="topbar card">
        <div class="user-block">
          <strong>{{ user?.username }}</strong>
          <small>当前角色 Active Role</small>
        </div>
        <div class="actions">
          <button type="button" class="locale-btn" @click="toggleLocale" :title="locale === 'zh-CN' ? 'Switch to English' : '切换中文'">
            {{ locale === 'zh-CN' ? 'EN' : '中' }}
          </button>
          <select
            aria-label="Switch role"
            :value="activeRole"
            @change="onSwitchRole"
          >
            <option v-for="role in user?.roles || []" :key="role" :value="role">
              {{ role }}
            </option>
          </select>
          <RouterLink class="outline-btn" to="/auth/change-password">修改密码</RouterLink>
          <button class="danger-btn" type="button" @click="onLogout">退出</button>
        </div>
      </header>

      <section class="page card" aria-live="polite">
        <RouterView />
      </section>
    </main>

    <!-- Toast container -->
    <div class="toast-container" aria-live="assertive">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        class="toast"
        :class="`toast-${toast.type}`"
      >
        {{ toast.message }}
      </div>
    </div>

    <!-- Session timeout warning -->
    <div
      v-if="sessionWarningVisible"
      class="modal-backdrop"
      role="dialog"
      aria-modal="true"
    >
      <div class="modal card">
        <h3>会话即将超时</h3>
        <p>25分钟未操作。点击"延长会话"保持登录状态。</p>
        <p><small>Session timeout warning at 25 minutes.</small></p>
        <button class="primary-btn" type="button" @click="auth.extendSession">
          延长会话 Extend Session
        </button>
      </div>
    </div>

    <!-- Concurrent session warning -->
    <div
      v-if="concurrentSessionDetected"
      class="modal-backdrop"
      role="dialog"
      aria-modal="true"
    >
      <div class="modal card concurrent-modal">
        <h3>检测到并发会话 Concurrent Session Detected</h3>
        <p>您的账号在其他设备或浏览器中登录。为保障账号安全，建议终止其他会话。</p>
        <p><small>Your account is active on another device/browser. For security, consider revoking other sessions.</small></p>
        <div v-if="activeSessions.length > 0" class="session-list">
          <div
            v-for="session in activeSessions"
            :key="session.sessionId"
            class="session-item"
            :class="{ 'current-session': session.current }"
          >
            <span>{{ session.device || 'Unknown device' }}</span>
            <small>{{ session.current ? '当前会话 Current' : session.lastActiveAt }}</small>
          </div>
        </div>
        <div class="modal-actions">
          <button class="danger-btn" type="button" @click="forceLogoutOthers">
            终止其他会话 Revoke Others
          </button>
          <button class="outline-btn" type="button" @click="dismissConcurrentWarning">
            暂时忽略 Dismiss
          </button>
        </div>
      </div>
    </div>

    <!-- Draft resume dialog -->
    <div
      v-if="draftDialogVisible"
      class="modal-backdrop"
      role="dialog"
      aria-modal="true"
    >
      <div class="modal card">
        <h3>检测到未保存草稿</h3>
        <p>是否继续上次编辑内容？</p>
        <p><small>Resume unsaved work?</small></p>
        <div class="modal-actions">
          <button class="primary-btn" type="button" @click="resumeDraft">
            继续 Continue
          </button>
          <button
            class="outline-btn"
            type="button"
            @click="draftDialogVisible = false"
          >
            稍后 Later
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.layout-shell {
  min-height: 100vh;
  padding: 18px;
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 16px;
}

.layout-shell.collapsed {
  grid-template-columns: 60px 1fr;
}

.collapsed .sidebar .brand,
.collapsed .sidebar .nav-item span,
.collapsed .sidebar .nav-item small,
.collapsed .sidebar .nav-badge {
  display: none;
}

.sidebar {
  padding: 16px;
  display: grid;
  grid-template-rows: auto auto 1fr;
  background: linear-gradient(180deg, #f9fdff 0%, #eef8ff 100%);
  position: relative;
  overflow: hidden;
  transition: width 0.2s;
}

.brand h1 {
  margin: 0;
  font-family: var(--font-heading);
  color: var(--color-primary-strong);
  font-size: 1.15rem;
}

.brand p {
  margin: 6px 0 18px;
  color: var(--color-text-soft);
  font-size: 0.82rem;
}

.collapse-btn {
  position: absolute;
  top: 10px;
  right: 6px;
  border: none;
  background: none;
  font-size: 0.9rem;
  cursor: pointer;
  color: var(--color-text-soft);
  min-height: auto;
  padding: 2px 6px;
}

nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nav-item {
  text-decoration: none;
  display: flex;
  flex-direction: column;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid transparent;
  position: relative;
}

.nav-item small {
  color: var(--color-text-soft);
}

.nav-item.active,
.nav-item:hover,
.nav-item:focus-visible {
  border-color: #9ac4d9;
  background: #e8f4fb;
  outline: none;
}

.nav-badge {
  position: absolute;
  top: 6px;
  right: 8px;
  background: var(--color-danger);
  color: white;
  font-size: 0.68rem;
  padding: 1px 6px;
  border-radius: 999px;
  font-weight: 600;
  min-width: 18px;
  text-align: center;
}

.content-area {
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 12px;
}

.topbar {
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.user-block {
  display: flex;
  flex-direction: column;
}

.user-block small {
  color: var(--color-text-soft);
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.locale-btn {
  min-height: 34px;
  min-width: 34px;
  border-radius: 8px;
  border: 1px solid var(--color-border);
  background: white;
  font-weight: 600;
  font-size: 0.82rem;
  cursor: pointer;
  padding: 0;
}

.actions select,
.primary-btn,
.outline-btn,
.danger-btn {
  border-radius: 10px;
  border: 1px solid var(--color-border);
  min-height: 38px;
  padding: 0 12px;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font: inherit;
}

.primary-btn {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
}

.outline-btn {
  background: white;
}

.danger-btn {
  background: #ffeceb;
  border-color: #f7c6c2;
  color: #8c2b2b;
}

.page {
  padding: 16px;
  overflow: auto;
}

/* Toast container */
.toast-container {
  position: fixed;
  top: 18px;
  right: 18px;
  z-index: 50;
  display: grid;
  gap: 8px;
  pointer-events: none;
}

.toast {
  padding: 10px 16px;
  border-radius: 10px;
  font-size: 0.88rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  pointer-events: auto;
  animation: slideIn 0.3s ease;
}

.toast-success {
  background: #e8f8ef;
  color: #2d8f57;
  border: 1px solid #98d6c4;
}

.toast-error {
  background: #fdeeed;
  color: #9e3a35;
  border: 1px solid #f7c6c2;
}

.toast-warning {
  background: #fef9e7;
  color: #b7791f;
  border: 1px solid #f0d78c;
}

.toast-info {
  background: #e7f3fa;
  color: var(--color-primary);
  border: 1px solid #9ac4d9;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* Modal styles */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(25, 40, 50, 0.32);
  display: grid;
  place-items: center;
  z-index: 20;
}

.modal {
  width: min(480px, calc(100vw - 28px));
  padding: 18px;
}

.modal h3 {
  margin: 0 0 8px;
}

.modal p {
  margin: 0 0 8px;
}

.modal-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

.concurrent-modal .session-list {
  display: grid;
  gap: 6px;
  margin: 10px 0;
}

.session-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  font-size: 0.88rem;
}

.session-item small {
  color: var(--color-text-soft);
}

.session-item.current-session {
  background: #e8f4fb;
  border-color: #9ac4d9;
}

@media (max-width: 980px) {
  .layout-shell {
    grid-template-columns: 1fr;
    padding: 10px;
  }

  .layout-shell.collapsed {
    grid-template-columns: 1fr;
  }

  .sidebar {
    grid-template-rows: auto;
  }

  .collapse-btn {
    display: none;
  }

  nav {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  }

  .nav-item {
    padding: 8px;
  }
}
</style>
