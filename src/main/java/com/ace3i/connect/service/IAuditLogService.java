package com.ace3i.connect.service;

import com.ace3i.connect.entity.AuditLog;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;
import java.util.List;

public interface IAuditLogService {
    void log(String action, String details);
    List<AuditLog> getAllLogs();
    Page<AuditLog> getLogs(int page, int size, String username, String action);
    void deleteLogsInPeriod(LocalDateTime start, LocalDateTime end);
}
