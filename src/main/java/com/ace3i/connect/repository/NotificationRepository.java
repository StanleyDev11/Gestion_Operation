package com.ace3i.connect.repository;

import com.ace3i.connect.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop10ByOrderByTimestampDesc();
    long countByIsReadFalse();
}
