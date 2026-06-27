package com.queueless.ai.repository;

import com.queueless.ai.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserIdOrderBySentAtDesc(Long userId, Pageable pageable);
}
