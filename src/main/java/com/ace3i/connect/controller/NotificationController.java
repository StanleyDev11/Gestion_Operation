package com.ace3i.connect.controller;

import com.ace3i.connect.entity.Notification;
import com.ace3i.connect.service.INotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private INotificationService notificationService;

    @GetMapping("/recent")
    public List<Notification> getRecent() {
        return notificationService.getRecentNotifications();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount() {
        return Map.of("count", notificationService.getUnreadCount());
    }

    @PostMapping("/mark-all-read")
    public void markAllAsRead() {
        notificationService.markAllAsRead();
    }
}
