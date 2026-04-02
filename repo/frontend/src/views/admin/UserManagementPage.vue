<script setup lang="ts">
import { reactive, ref, computed } from "vue";
import DataTable from "@/components/DataTable/DataTable.vue";
import { api, unwrap } from "@/api";
import { handleApiError, showSuccess } from "@/utils/toast";
import { useI18n } from "@/i18n";
import type { PageData } from "@/types/api";
import type { TableColumn } from "@/types/ui";

const { t } = useI18n();
const filters = reactive({ role: "", status: "", search: "" });

const columns: TableColumn[] = [
  { key: "username", label: "账号 Username", sortable: true },
  { key: "displayName", label: "显示名称 Display Name", sortable: true },
  { key: "roles", label: "角色 Roles", sortable: false },
  { key: "status", label: "状态 Status", sortable: true },
  { key: "studentId", label: "学号 Student ID", sortable: false, maskPii: true },
  { key: "createdAt", label: "创建时间 Created At", sortable: true },
];

const roleOptions = ["ADMIN", "ACADEMIC_AFFAIRS", "HOMEROOM_TEACHER", "SUBJECT_TEACHER", "STUDENT"];
const statusOptions = ["ACTIVE", "LOCKED", "DISABLED"];

const showCreateDialog = ref(false);
const showEditDialog = ref(false);
const showDeleteConfirm = ref(false);
const editingUser = ref<Record<string, unknown> | null>(null);
const deletingUser = ref<Record<string, unknown> | null>(null);
const tableKey = ref(0);

const createForm = reactive({
  username: "",
  password: "",
  displayName: "",
  role: "STUDENT",
  gradeId: "",
  classId: "",
  courseId: "",
  termId: "",
});

const editForm = reactive({
  roles: [] as string[],
  status: "ACTIVE",
  gradeId: "",
  classId: "",
});

const passwordStrength = computed(() => {
  const p = createForm.password;
  if (!p) return { level: 0, label: "", cls: "" };
  let score = 0;
  if (p.length >= 8) score++;
  if (/[A-Z]/.test(p)) score++;
  if (/[0-9]/.test(p)) score++;
  if (/[^A-Za-z0-9]/.test(p)) score++;
  if (score <= 1) return { level: 1, label: t("users.weak"), cls: "weak" };
  if (score <= 2) return { level: 2, label: t("users.medium"), cls: "medium" };
  return { level: 3, label: t("users.strong"), cls: "strong" };
});

async function fetcher(
  params: Record<string, unknown>,
): Promise<PageData<Record<string, unknown>>> {
  const data = await unwrap(
    api.get("/users", { params: { ...params, ...filters } }),
  );
  const pagination = data.pagination || {};
  return {
    items: data.items || [],
    pagination: {
      page: pagination.page || Number(params.page) || 1,
      size: pagination.size || Number(params.size) || 20,
      totalItems: pagination.total || pagination.totalItems || 0,
      totalPages: pagination.totalPages || 0,
    },
  };
}

async function createUser(): Promise<void> {
  try {
    await unwrap(
      api.post("/users", {
        username: createForm.username,
        password: createForm.password,
        displayName: createForm.displayName,
        role: createForm.role,
        scopes: {
          gradeIds: createForm.gradeId ? [Number(createForm.gradeId)] : [],
          classIds: createForm.classId ? [Number(createForm.classId)] : [],
          subjectIds: createForm.courseId ? [Number(createForm.courseId)] : [],
        },
      }),
    );
    showSuccess(t("common.success"));
    showCreateDialog.value = false;
    resetCreateForm();
    tableKey.value++;
  } catch (err) {
    handleApiError(err);
  }
}

function resetCreateForm() {
  createForm.username = "";
  createForm.password = "";
  createForm.displayName = "";
  createForm.role = "STUDENT";
  createForm.gradeId = "";
  createForm.classId = "";
  createForm.courseId = "";
  createForm.termId = "";
}

function openEdit(row: Record<string, unknown>) {
  editingUser.value = row;
  const roles = row.roles;
  editForm.roles = Array.isArray(roles) ? [...roles] : [String(roles || "")];
  editForm.status = String(row.status || "ACTIVE");
  showEditDialog.value = true;
}

async function saveEdit(): Promise<void> {
  if (!editingUser.value) return;
  try {
    await unwrap(
      api.put(`/users/${editingUser.value.id}`, {
        roles: editForm.roles,
        status: editForm.status,
        scopes: {
          gradeIds: editForm.gradeId ? [Number(editForm.gradeId)] : [],
          classIds: editForm.classId ? [Number(editForm.classId)] : [],
        },
      }),
    );
    showSuccess(t("common.success"));
    showEditDialog.value = false;
    tableKey.value++;
  } catch (err) {
    handleApiError(err);
  }
}

function confirmDelete(row: Record<string, unknown>) {
  deletingUser.value = row;
  showDeleteConfirm.value = true;
}

async function softDelete(): Promise<void> {
  if (!deletingUser.value) return;
  try {
    await unwrap(api.delete(`/users/${deletingUser.value.id}`));
    showSuccess(t("common.success"));
    showDeleteConfirm.value = false;
    tableKey.value++;
  } catch (err) {
    handleApiError(err);
  }
}

async function unlock(row: Record<string, unknown>): Promise<void> {
  try {
    await unwrap(api.post(`/users/${row.id}/unlock`));
    showSuccess(t("common.success"));
    tableKey.value++;
  } catch (err) {
    handleApiError(err);
  }
}

async function toggleConcurrentSessions(row: Record<string, unknown>): Promise<void> {
  try {
    await unwrap(api.post(`/users/${row.id}/toggle-concurrent-sessions`));
    showSuccess(t("common.success"));
    tableKey.value++;
  } catch (err) {
    handleApiError(err);
  }
}

function toggleRole(role: string) {
  const idx = editForm.roles.indexOf(role);
  if (idx >= 0) editForm.roles.splice(idx, 1);
  else editForm.roles.push(role);
}
</script>

<template>
  <section class="page-grid">
    <nav class="breadcrumb" aria-label="Breadcrumb">
      <span>{{ t("nav.dashboard") }}</span> / <span>{{ t("nav.users") }}</span>
    </nav>

    <header class="head-row">
      <div>
        <h2>{{ t("users.title") }}</h2>
        <p>{{ t("users.subtitle") }}</p>
      </div>
      <button type="button" class="primary-btn" @click="showCreateDialog = true">
        {{ t("users.createUser") }}
      </button>
    </header>

    <DataTable
      :key="tableKey"
      :columns="columns"
      :fetcher="fetcher"
      :filters="filters"
      :search-placeholder="t('users.searchPlaceholder')"
    >
      <template #filters>
        <div class="filters-row">
          <select v-model="filters.role" aria-label="Filter by role">
            <option value="">{{ t("users.filterByRole") }}</option>
            <option v-for="r in roleOptions" :key="r" :value="r">{{ r }}</option>
          </select>
          <select v-model="filters.status" aria-label="Filter by status">
            <option value="">{{ t("users.filterByStatus") }}</option>
            <option v-for="s in statusOptions" :key="s" :value="s">{{ s }}</option>
          </select>
        </div>
      </template>
      <template #cell-roles="{ row }">
        <span class="role-tags">
          <span
            v-for="r in (Array.isArray(row.roles) ? row.roles : [row.roles || row.activeRole])"
            :key="r"
            class="tag"
          >{{ r }}</span>
        </span>
      </template>
      <template #cell-status="{ row }">
        <span
          class="status-badge"
          :class="{
            'badge-green': row.status === 'ACTIVE',
            'badge-red': row.status === 'LOCKED',
            'badge-gray': row.status === 'DISABLED',
          }"
        >{{ row.status }}</span>
      </template>
      <template #actions="{ row }">
        <div class="row-actions">
          <button type="button" @click.stop="openEdit(row)">{{ t("common.edit") }}</button>
          <button type="button" @click.stop="confirmDelete(row)">{{ t("users.softDelete") }}</button>
          <button type="button" @click.stop="unlock(row)">{{ t("users.unlock") }}</button>
          <button type="button" @click.stop="toggleConcurrentSessions(row)">{{ t("users.toggleSessions") }}</button>
        </div>
      </template>
    </DataTable>

    <!-- Create User Dialog -->
    <div v-if="showCreateDialog" class="modal-backdrop" role="dialog" aria-modal="true">
      <div class="modal card">
        <h3>{{ t("users.createUser") }}</h3>
        <form class="form-grid" @submit.prevent="createUser" @keydown.esc="showCreateDialog = false">
          <label class="field">
            <span>{{ t("users.username") }}</span>
            <input v-model="createForm.username" type="text" required autofocus />
          </label>
          <label class="field">
            <span>{{ t("users.password") }}</span>
            <input v-model="createForm.password" type="password" required />
            <div v-if="createForm.password" class="strength-meter">
              <div class="strength-bar" :class="passwordStrength.cls">
                <div class="strength-fill" :style="{ width: (passwordStrength.level / 3) * 100 + '%' }" />
              </div>
              <small>{{ t("users.passwordStrength") }}: {{ passwordStrength.label }}</small>
            </div>
          </label>
          <label class="field">
            <span>{{ t("users.displayName") }}</span>
            <input v-model="createForm.displayName" type="text" required />
          </label>
          <label class="field">
            <span>{{ t("users.roleSelection") }}</span>
            <select v-model="createForm.role">
              <option v-for="r in roleOptions" :key="r" :value="r">{{ r }}</option>
            </select>
          </label>
          <label class="field">
            <span>年级ID Grade</span>
            <input v-model="createForm.gradeId" type="number" />
          </label>
          <label class="field">
            <span>班级ID Class</span>
            <input v-model="createForm.classId" type="number" />
          </label>
          <label class="field">
            <span>课程ID Course</span>
            <input v-model="createForm.courseId" type="number" />
          </label>
          <label class="field">
            <span>学期ID Term</span>
            <input v-model="createForm.termId" type="number" />
          </label>
          <div class="modal-actions">
            <button type="submit" class="primary-btn">{{ t("common.create") }}</button>
            <button type="button" class="outline-btn" @click="showCreateDialog = false">{{ t("common.cancel") }}</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Edit User Dialog -->
    <div v-if="showEditDialog && editingUser" class="modal-backdrop" role="dialog" aria-modal="true">
      <div class="modal card">
        <h3>{{ t("users.editUser") }}: {{ editingUser.username }}</h3>
        <form class="form-grid" @submit.prevent="saveEdit" @keydown.esc="showEditDialog = false">
          <div class="field">
            <span>{{ t("users.roleSelection") }}</span>
            <div class="checkbox-group">
              <label v-for="r in roleOptions" :key="r" class="checkbox-item">
                <input type="checkbox" :checked="editForm.roles.includes(r)" @change="toggleRole(r)" />
                {{ r }}
              </label>
            </div>
          </div>
          <label class="field">
            <span>{{ t("users.status") }}</span>
            <select v-model="editForm.status">
              <option v-for="s in statusOptions" :key="s" :value="s">{{ s }}</option>
            </select>
          </label>
          <label class="field">
            <span>年级ID Grade</span>
            <input v-model="editForm.gradeId" type="number" />
          </label>
          <label class="field">
            <span>班级ID Class</span>
            <input v-model="editForm.classId" type="number" />
          </label>
          <div class="modal-actions">
            <button type="submit" class="primary-btn">{{ t("common.save") }}</button>
            <button type="button" class="outline-btn" @click="showEditDialog = false">{{ t("common.cancel") }}</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Delete Confirm Dialog -->
    <div v-if="showDeleteConfirm" class="modal-backdrop" role="dialog" aria-modal="true">
      <div class="modal card">
        <h3>{{ t("users.softDelete") }}</h3>
        <p>{{ t("users.confirmDelete") }}</p>
        <p v-if="deletingUser"><strong>{{ deletingUser.username }}</strong></p>
        <div class="modal-actions">
          <button type="button" class="danger-btn" @click="softDelete">{{ t("common.confirm") }}</button>
          <button type="button" class="outline-btn" @click="showDeleteConfirm = false">{{ t("common.cancel") }}</button>
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

.filters-row {
  display: flex;
  gap: 6px;
}

select, input, button {
  min-height: 36px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  padding: 0 10px;
  font: inherit;
}

.row-actions {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.role-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.tag {
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: 6px;
  background: #e7f3fa;
  color: var(--color-primary);
}

.status-badge {
  font-size: 0.8rem;
  padding: 2px 10px;
  border-radius: 999px;
  font-weight: 500;
}

.badge-green {
  background: #e8f8ef;
  color: #2d8f57;
}

.badge-red {
  background: #fdeeed;
  color: #9e3a35;
}

.badge-gray {
  background: #f0f2f4;
  color: #6b7280;
}

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

.modal h3 {
  margin: 0 0 12px;
}

.form-grid {
  display: grid;
  gap: 10px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field input,
.field select {
  width: 100%;
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

.strength-bar.weak .strength-fill {
  background: var(--color-danger);
}

.strength-bar.medium .strength-fill {
  background: var(--color-warning);
}

.strength-bar.strong .strength-fill {
  background: var(--color-success);
}

.checkbox-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.checkbox-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.85rem;
}

.checkbox-item input[type="checkbox"] {
  min-height: auto;
}

.modal-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.primary-btn {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
  cursor: pointer;
}

.outline-btn {
  background: white;
  cursor: pointer;
}

.danger-btn {
  background: #ffeceb;
  border-color: #f7c6c2;
  color: #8c2b2b;
  cursor: pointer;
}
</style>
