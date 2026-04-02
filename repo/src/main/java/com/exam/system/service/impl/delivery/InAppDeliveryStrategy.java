package com.exam.system.service.impl.delivery;

import com.exam.system.entity.NotifDelivery;
import com.exam.system.entity.Notification;
import com.exam.system.entity.NotificationChannel;
import com.exam.system.entity.SysUser;
import org.springframework.stereotype.Component;

@Component
public class InAppDeliveryStrategy implements NotificationDeliveryStrategy {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.APP;
    }

    @Override
    public boolean supports(SysUser user) {
        return true;
    }

    @Override
    public DeliveryAttemptResult deliver(Notification notification, NotifDelivery delivery, SysUser user) {
        return DeliveryAttemptResult.success();
    }
}
