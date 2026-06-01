package com.ace3i.connect.service.impl;

import com.ace3i.connect.entity.Notification;
import com.ace3i.connect.repository.NotificationRepository;
import com.ace3i.connect.service.INotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NotificationService implements INotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public void createNotification(String title, String message, String type, String url) {
        Notification notification = new Notification(title, message, type, url);
        notificationRepository.save(notification);
    }

    @Override
    public List<Notification> getRecentNotifications() {
        return notificationRepository.findTop10ByOrderByTimestampDesc();
    }

    @Override
    public long getUnreadCount() {
        return notificationRepository.countByIsReadFalse();
    }

    @Override
    public void markAllAsRead() {
        List<Notification> unread = notificationRepository.findAll().stream()
                .filter(n -> !n.isRead())
                .toList();
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}
