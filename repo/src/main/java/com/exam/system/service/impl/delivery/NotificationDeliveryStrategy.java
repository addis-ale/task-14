package com.exam.system.service.impl.delivery;

import com.exam.system.entity.NotifDelivery;
import com.exam.system.entity.Notification;
import com.exam.system.entity.NotificationChannel;
import com.exam.system.entity.SysUser;

public interface NotificationDeliveryStrategy {

    NotificationChannel channel();

    boolean supports(SysUser user);

    DeliveryAttemptResult deliver(Notification notification, NotifDelivery delivery, SysUser user);
}
