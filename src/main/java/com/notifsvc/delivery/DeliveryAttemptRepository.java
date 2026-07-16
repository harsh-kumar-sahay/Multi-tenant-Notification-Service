package com.notifsvc.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, Long> {
    List<DeliveryAttempt> findByNotificationIdOrderByAttemptNumberAsc(Long notificationId);
}
