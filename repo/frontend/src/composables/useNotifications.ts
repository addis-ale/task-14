import { onMounted } from "vue";
import { storeToRefs } from "pinia";
import { useNotificationsStore } from "@/stores/notifications";

export function useNotifications() {
  const store = useNotificationsStore();
  const { inbox, unread, loading, hasUnread } = storeToRefs(store);

  onMounted(async () => {
    await Promise.all([store.fetchInbox(), store.fetchUnreadCount()]);
  });

  return {
    inbox,
    unread,
    loading,
    hasUnread,
    fetchInbox: store.fetchInbox,
    markRead: store.markRead,
    fetchUnreadCount: store.fetchUnreadCount,
  };
}
