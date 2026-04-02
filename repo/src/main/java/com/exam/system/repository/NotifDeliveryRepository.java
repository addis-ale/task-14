package com.exam.system.repository;

import com.exam.system.entity.NotifDelivery;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotifDeliveryRepository extends JpaRepository<NotifDelivery, Long> {

    Page<NotifDelivery> findByNotificationId(Long notificationId, Pageable pageable);

    Page<NotifDelivery> findByNotificationIdAndStatus(Long notificationId, String status, Pageable pageable);

    long countByNotificationId(Long notificationId);

    long countByNotificationIdAndStatus(Long notificationId, String status);

    Page<NotifDelivery> findByRecipientId(Long recipientId, Pageable pageable);

    Page<NotifDelivery> findByRecipientIdAndStatus(Long recipientId, String status, Pageable pageable);

    Page<NotifDelivery> findByRecipientIdAndReadAtIsNull(Long recipientId, Pageable pageable);

    Page<NotifDelivery> findByRecipientIdAndStatusAndReadAtIsNull(Long recipientId, String status, Pageable pageable);

    Page<NotifDelivery> findByRecipientIdAndReadAtIsNotNull(Long recipientId, Pageable pageable);

    Page<NotifDelivery> findByRecipientIdAndStatusAndReadAtIsNotNull(Long recipientId, String status, Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    long countByRecipientIdAndStatusAndReadAtIsNull(Long recipientId, String status);

    List<NotifDelivery> findByRecipientIdAndReadAtIsNull(Long recipientId);

    List<NotifDelivery> findByRecipientIdAndStatusAndReadAtIsNull(Long recipientId, String status);

    List<NotifDelivery> findByStatusAndDeliveredAtIsNullAndAttemptsLessThan(String status, Integer attempts);

    List<NotifDelivery> findByStatus(String status);
}
