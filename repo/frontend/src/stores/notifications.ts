import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { api, unwrap } from "@/api";
import type { PageData } from "@/types/api";

export interface InboxMessage {
  deliveryId: number;
  notificationId: number;
  title: string;
  body: string;
  eventType: string;
  priority: string;
  deliveredAt: string;
  read: boolean;
}

export const useNotificationsStore = defineStore("notifications", () => {
  const inbox = ref<PageData<InboxMessage> | null>(null);
  const unread = ref(0);
  const loading = ref(false);

  const hasUnread = computed(() => unread.value > 0);

  async function fetchInbox(
    page = 1,
    size = 20,
    read?: boolean,
  ): Promise<void> {
    loading.value = true;
    try {
      inbox.value = await unwrap(
        api.get<PageData<InboxMessage>>("/inbox", {
          params: { page, size, read },
        }),
      );
    } finally {
      loading.value = false;
    }
  }

  async function fetchUnreadCount(): Promise<void> {
    const data = await unwrap(
      api.get<{ unreadCount: number }>("/inbox/unread-count"),
    );
    unread.value = data.unreadCount;
  }

  async function markRead(deliveryId: number): Promise<void> {
    await unwrap(api.put(`/inbox/${deliveryId}/read`));
    await fetchUnreadCount();
  }

  return {
    inbox,
    unread,
    loading,
    hasUnread,
    fetchInbox,
    fetchUnreadCount,
    markRead,
  };
});
