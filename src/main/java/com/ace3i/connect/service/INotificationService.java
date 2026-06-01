package com.ace3i.connect.service;

import com.ace3i.connect.entity.Notification;
import java.util.List;

public interface INotificationService {
    void createNotification(String title, String message, String type, String url);
    List<Notification> getRecentNotifications();
    long getUnreadCount();
    void markAllAsRead();
}
