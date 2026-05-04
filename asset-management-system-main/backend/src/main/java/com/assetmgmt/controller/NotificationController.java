package com.assetmgmt.controller;

import com.assetmgmt.entity.Notification;
import com.assetmgmt.entity.User;
import com.assetmgmt.repository.UserRepository;
import com.assetmgmt.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<Page<Notification>> getMyNotifications(Pageable pageable) {
        User user = getCurrentUser();
        return ResponseEntity.ok(notificationService.getUserNotifications(user.getId(), pageable));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount() {
        User user = getCurrentUser();
        return ResponseEntity.ok(notificationService.getUnreadCount(user.getId()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        User user = getCurrentUser();
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok().build();
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }
}
