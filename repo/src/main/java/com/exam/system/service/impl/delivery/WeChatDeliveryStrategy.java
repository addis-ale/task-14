package com.exam.system.service.impl.delivery;

import com.exam.system.entity.NotifDelivery;
import com.exam.system.entity.Notification;
import com.exam.system.entity.NotificationChannel;
import com.exam.system.entity.SysUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WeChatDeliveryStrategy implements NotificationDeliveryStrategy {

    private final boolean enabled;

    public WeChatDeliveryStrategy(@Value("${app.wechat.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.WECHAT;
    }

    @Override
    public boolean supports(SysUser user) {
        return enabled && user != null && user.getDeviceToken() != null && !user.getDeviceToken().isBlank();
    }

    @Override
    public DeliveryAttemptResult deliver(Notification notification, NotifDelivery delivery, SysUser user) {
        if (!enabled) {
            return DeliveryAttemptResult.unavailable("wechat disabled");
        }
        if (user.getDeviceToken() == null || user.getDeviceToken().isBlank()) {
            return DeliveryAttemptResult.unavailable("wechat token unavailable");
        }
        return DeliveryAttemptResult.success();
    }
}
