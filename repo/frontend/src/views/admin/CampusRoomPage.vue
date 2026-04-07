<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { api, unwrap } from "@/api";
import { handleApiError, showSuccess } from "@/utils/toast";
import { useI18n } from "@/i18n";
import { useRBAC } from "@/composables/useRBAC";

const { t } = useI18n();
const { can } = useRBAC();

interface Room {
  id: number;
  name: string;
  capacity: number;
  usedCapacity?: number;
}

interface Campus {
  id: number;
  name: string;
  rooms: Room[];
  expanded?: boolean;
}

const loading = ref(false);
const campuses = ref<Campus[]>([]);

// Campus dialog
const showCampusDialog = ref(false);
const editingCampusId = ref<number | null>(null);
const campusForm = reactive({ name: "" });

// Room dialog
const showRoomDialog = ref(false);
const editingRoomId = ref<number | null>(null);
const roomCampusId = ref<number | null>(null);
const roomForm = reactive({ name: "", capacity: 30 });

async function loadCampuses() {
  loading.value = true;
  try {
    const data = await unwrap(api.get("/campuses"));
    const items = data.items || data || [];
    campuses.value = items.map((c: Campus) => ({ ...c, expanded: false, rooms: c.rooms || [] }));
  } catch (err) {
    handleApiError(err);
  } finally {
    loading.value = false;
  }
}

function toggleExpand(campus: Campus) {
  campus.expanded = !campus.expanded;
}

// Campus CRUD
function openAddCampus() {
  editingCampusId.value = null;
  campusForm.name = "";
  showCampusDialog.value = true;
}

function openEditCampus(campus: Campus) {
  editingCampusId.value = campus.id;
  campusForm.name = campus.name;
  showCampusDialog.value = true;
}

async function saveCampus() {
  try {
    if (editingCampusId.value) {
      await unwrap(api.put(`/campuses/${editingCampusId.value}`, { name: campusForm.name }));
    } else {
      await unwrap(api.post("/campuses", { name: campusForm.name }));
    }
    showSuccess(t("common.success"));
    showCampusDialog.value = false;
    await loadCampuses();
  } catch (err) {
    handleApiError(err);
  }
}

// Room CRUD
function openAddRoom(campusId: number) {
  editingRoomId.value = null;
  roomCampusId.value = campusId;
  roomForm.name = "";
  roomForm.capacity = 30;
  showRoomDialog.value = true;
}

function openEditRoom(campusId: number, room: Room) {
  editingRoomId.value = room.id;
  roomCampusId.value = campusId;
  roomForm.name = room.name;
  roomForm.capacity = room.capacity;
  showRoomDialog.value = true;
}

async function saveRoom() {
  try {
    if (editingRoomId.value) {
      await unwrap(
        api.put(`/rooms/${editingRoomId.value}`, {
          name: roomForm.name,
          capacity: roomForm.capacity,
        }),
      );
    } else {
      await unwrap(
        api.post(`/campuses/${roomCampusId.value}/rooms`, {
          name: roomForm.name,
          capacity: roomForm.capacity,
        }),
      );
    }
    showSuccess(t("common.success"));
    showRoomDialog.value = false;
    await loadCampuses();
  } catch (err) {
    handleApiError(err);
  }
}

function capacityPercent(room: Room): number {
  if (!room.capacity) return 0;
  return Math.min(100, Math.round(((room.usedCapacity || 0) / room.capacity) * 100));
}

onMounted(() => {
  void loadCampuses();
});
</script>

<template>
  <section class="page-grid">
    <nav class="breadcrumb" aria-label="Breadcrumb">
      <span>{{ t("nav.dashboard") }}</span> / <span>{{ t("nav.campusRooms") }}</span>
    </nav>

    <header class="head-row">
      <div>
        <h2>{{ t("campus.title") }}</h2>
        <p>{{ t("campus.subtitle") }}</p>
      </div>
      <button type="button" class="primary-btn" @click="openAddCampus">
        {{ t("campus.addCampus") }}
      </button>
    </header>

    <div v-if="loading" class="card empty">{{ t("common.loading") }}</div>

    <div v-else-if="campuses.length === 0" class="card empty">
      <p class="illu">🏫</p>
      <p>{{ t("common.noData") }}</p>
    </div>

    <div v-else class="campus-list">
      <div v-for="campus in campuses" :key="campus.id" class="card campus-card">
        <div class="campus-header" @click="toggleExpand(campus)">
          <div class="campus-info">
            <span class="expand-icon">{{ campus.expanded ? '▾' : '▸' }}</span>
            <strong>{{ campus.name }}</strong>
            <small>{{ campus.rooms.length }} {{ t("campus.roomName") }}</small>
          </div>
          <div class="campus-actions" @click.stop>
            <button v-if="can('update')" type="button" @click="openEditCampus(campus)">{{ t("common.edit") }}</button>
            <button v-if="can('create')" type="button" @click="openAddRoom(campus.id)">{{ t("campus.addRoom") }}</button>
          </div>
        </div>

        <div v-if="campus.expanded" class="room-list">
          <div v-if="campus.rooms.length === 0" class="room-empty">
            {{ t("common.noData") }}
          </div>
          <div v-for="room in campus.rooms" :key="room.id" class="room-row">
            <div class="room-info">
              <span>{{ room.name }}</span>
              <small>{{ t("campus.capacity") }}: {{ room.capacity }}</small>
            </div>
            <div class="capacity-bar-wrapper">
              <div class="capacity-bar">
                <div
                  class="capacity-fill"
                  :style="{ width: capacityPercent(room) + '%' }"
                  :class="{
                    'fill-green': capacityPercent(room) < 70,
                    'fill-yellow': capacityPercent(room) >= 70 && capacityPercent(room) < 90,
                    'fill-red': capacityPercent(room) >= 90,
                  }"
                />
              </div>
              <small>{{ room.usedCapacity || 0 }}/{{ room.capacity }}</small>
            </div>
            <button v-if="can('update')" type="button" @click="openEditRoom(campus.id, room)">{{ t("common.edit") }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Campus Dialog -->
    <div v-if="showCampusDialog && can(editingCampusId ? 'update' : 'create')" class="modal-backdrop" role="dialog" aria-modal="true">
      <div class="modal card">
        <h3>{{ editingCampusId ? t("campus.editCampus") : t("campus.addCampus") }}</h3>
        <form @submit.prevent="saveCampus" @keydown.esc="showCampusDialog = false">
          <label class="field">
            <span>{{ t("campus.campusName") }}</span>
            <input v-model="campusForm.name" type="text" required autofocus />
          </label>
          <div class="modal-actions">
            <button type="submit" class="primary-btn">{{ t("common.save") }}</button>
            <button type="button" class="outline-btn" @click="showCampusDialog = false">{{ t("common.cancel") }}</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Room Dialog -->
    <div v-if="showRoomDialog && can(editingRoomId ? 'update' : 'create')" class="modal-backdrop" role="dialog" aria-modal="true">
      <div class="modal card">
        <h3>{{ editingRoomId ? t("campus.editRoom") : t("campus.addRoom") }}</h3>
        <form @submit.prevent="saveRoom" @keydown.esc="showRoomDialog = false">
          <label class="field">
            <span>{{ t("campus.roomName") }}</span>
            <input v-model="roomForm.name" type="text" required autofocus />
          </label>
          <label class="field">
            <span>{{ t("campus.capacity") }}</span>
            <input v-model.number="roomForm.capacity" type="number" min="1" required />
          </label>
          <div class="modal-actions">
            <button type="submit" class="primary-btn">{{ t("common.save") }}</button>
            <button type="button" class="outline-btn" @click="showRoomDialog = false">{{ t("common.cancel") }}</button>
          </div>
        </form>
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

.empty {
  padding: 24px;
  text-align: center;
}

.illu {
  font-size: 2.4rem;
  margin: 0;
}

.campus-list {
  display: grid;
  gap: 10px;
}

.campus-card {
  overflow: hidden;
}

.campus-header {
  padding: 12px 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  user-select: none;
}

.campus-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.expand-icon {
  font-size: 0.9rem;
  width: 16px;
}

.campus-info small {
  color: var(--color-text-soft);
}

.campus-actions {
  display: flex;
  gap: 6px;
}

.room-list {
  border-top: 1px solid var(--color-border);
  padding: 8px 14px;
}

.room-empty {
  padding: 10px;
  text-align: center;
  color: var(--color-text-soft);
  font-size: 0.85rem;
}

.room-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f4f7;
}

.room-row:last-child {
  border-bottom: none;
}

.room-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.room-info small {
  color: var(--color-text-soft);
  font-size: 0.8rem;
}

.capacity-bar-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 140px;
}

.capacity-bar {
  flex: 1;
  height: 8px;
  border-radius: 4px;
  background: #e5e7eb;
  overflow: hidden;
}

.capacity-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s;
}

.fill-green { background: var(--color-success); }
.fill-yellow { background: var(--color-warning); }
.fill-red { background: var(--color-danger); }

.capacity-bar-wrapper small {
  font-size: 0.78rem;
  color: var(--color-text-soft);
  white-space: nowrap;
}

button {
  min-height: 34px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  padding: 0 10px;
  font: inherit;
  cursor: pointer;
  background: white;
}

.primary-btn {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
}

.outline-btn {
  background: white;
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
  width: min(440px, calc(100vw - 28px));
  padding: 18px;
}

.modal h3 {
  margin-bottom: 12px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 10px;
}

.field input {
  width: 100%;
}

.modal-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
</style>
