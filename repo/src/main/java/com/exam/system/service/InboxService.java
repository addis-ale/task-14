package com.exam.system.service;

import com.exam.system.dto.common.PageData;
import com.exam.system.dto.inbox.InboxMessageResponse;

public interface InboxService {

    PageData<InboxMessageResponse> list(Long userId, Boolean read, int page, int size);

    void markRead(Long userId, Long deliveryId);

    void markAllRead(Long userId);

    long unreadCount(Long userId);
}
