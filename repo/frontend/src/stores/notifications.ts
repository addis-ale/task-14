import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { api, unwrap } from "@/api";
import { handleApiError, showSuccess } from "@/utils/toast";
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

export interface NotificationItem {
  id: number;
  title: string;
  body: string;
  eventType: string;
  priority: string;
  status: string;
  complianceStatus?: string;
  publishedBy?: string;
  createdAt: string;
}

export interface CreateNotificationPayload {
  title: string;
  body: string;
  eventType: string;
  priority: string;
  targetGradeIds?: number[];
  targetClassIds?: number[];
}

export const useNotificationsStore = defineStore("notifications", () => {
  const inbox = ref<PageData<InboxMessage> | null>(null);
  const unread = ref(0);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const notifications = ref<PageData<NotificationItem> | null>(null);

  const hasUnread = computed(() => unread.value > 0);
  const inboxItems = computed(() => inbox.value?.items || []);
  const notificationItems = computed(() => notifications.value?.items || []);
  const isEmpty = computed(() => !loading.value && inboxItems.value.length === 0);

  async function fetchInbox(
    page = 1,
    size = 20,
    read?: boolean,
  ): Promise<void> {
    loading.value = true;
    error.value = null;
    try {
      inbox.value = await unwrap(
        api.get<PageData<InboxMessage>>("/inbox", {
          params: { page, size, read },
        }),
      );
    } catch (err) {
      error.value = "加载消息失败 Failed to load inbox";
      handleApiError(err);
    } finally {
      loading.value = false;
    }
  }

  async function fetchUnreadCount(): Promise<void> {
    try {
      const data = await unwrap(
        api.get<{ unreadCount: number }>("/inbox/unread-count"),
      );
      unread.value = data.unreadCount;
    } catch {
      // silent — badge count is non-critical
    }
  }

  async function markRead(deliveryId: number): Promise<boolean> {
    error.value = null;
    try {
      await unwrap(api.put(`/inbox/${deliveryId}/read`));
      // Update local state immediately
      if (inbox.value?.items) {
        const msg = inbox.value.items.find((m) => m.deliveryId === deliveryId);
        if (msg) msg.read = true;
      }
      await fetchUnreadCount();
      return true;
    } catch (err) {
      error.value = "标记已读失败 Failed to mark as read";
      handleApiError(err);
      return false;
    }
  }

  async function markAllRead(): Promise<boolean> {
    error.value = null;
    try {
      await unwrap(api.put("/inbox/read-all"));
      if (inbox.value?.items) {
        inbox.value.items.forEach((m) => { m.read = true; });
      }
      unread.value = 0;
      showSuccess("全部标记已读 All marked as read");
      return true;
    } catch (err) {
      error.value = "操作失败 Operation failed";
      handleApiError(err);
      return false;
    }
  }

  async function fetchNotifications(
    params: Record<string, unknown> = {},
  ): Promise<void> {
    loading.value = true;
    error.value = null;
    try {
      notifications.value = await unwrap(
        api.get<PageData<NotificationItem>>("/notifications", { params }),
      );
    } catch (err) {
      error.value = "加载通知列表失败 Failed to load notifications";
      handleApiError(err);
    } finally {
      loading.value = false;
    }
  }

  async function createNotification(
    payload: CreateNotificationPayload,
  ): Promise<NotificationItem | null> {
    loading.value = true;
    error.value = null;
    try {
      const created = await unwrap(
        api.post<NotificationItem>("/notifications", payload),
      );
      showSuccess("通知创建成功 Notification created");
      return created;
    } catch (err) {
      error.value = "创建通知失败 Failed to create notification";
      handleApiError(err);
      return null;
    } finally {
      loading.value = false;
    }
  }

  async function submitForReview(notificationId: number): Promise<boolean> {
    error.value = null;
    try {
      await unwrap(api.post(`/notifications/${notificationId}/submit-review`));
      showSuccess("已提交审核 Submitted for review");
      return true;
    } catch (err) {
      error.value = "提交审核失败 Failed to submit for review";
      handleApiError(err);
      return false;
    }
  }

  async function publishNotification(notificationId: number): Promise<boolean> {
    error.value = null;
    try {
      await unwrap(api.post(`/notifications/${notificationId}/publish`));
      showSuccess("通知已发布 Notification published");
      return true;
    } catch (err) {
      error.value = "发布失败 Failed to publish";
      handleApiError(err);
      return false;
    }
  }

  function clearError(): void {
    error.value = null;
  }

  return {
    inbox,
    unread,
    loading,
    error,
    notifications,
    hasUnread,
    inboxItems,
    notificationItems,
    isEmpty,
    fetchInbox,
    fetchUnreadCount,
    markRead,
    markAllRead,
    fetchNotifications,
    createNotification,
    submitForReview,
    publishNotification,
    clearError,
  };
});
