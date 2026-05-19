package com.assetmgmt.repository;

import com.assetmgmt.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    long countByUserIdAndReadFalse(Long userId);
    List<Notification> findByUserIdAndReadFalse(Long userId);
    boolean existsByUserIdAndMessageAndType(Long userId, String message, String type);
}
