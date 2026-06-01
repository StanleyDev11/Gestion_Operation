package com.ace3i.connect.service.impl;

import com.ace3i.connect.entity.AuditLog;
import com.ace3i.connect.repository.AuditLogRepository;
import com.ace3i.connect.service.IAuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AuditLogService implements IAuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private HttpServletRequest request;

    @Override
    public void log(String action, String details) {
        String username = SecurityContextHolder.getContext().getAuthentication() != null 
            ? SecurityContextHolder.getContext().getAuthentication().getName() 
            : "Système";
        
        String ipAddress = request.getRemoteAddr();
        
        AuditLog auditLog = new AuditLog(username, action, details, ipAddress);
        auditLogRepository.save(auditLog);
    }

    @Override
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

    @Override
    public Page<AuditLog> getLogs(int page, int size, String username, String action) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        
        if (username != null && !username.isEmpty() && action != null && !action.isEmpty()) {
            return auditLogRepository.findByUsernameContainingAndActionContaining(username, action, pageable);
        } else if (username != null && !username.isEmpty()) {
            return auditLogRepository.findByUsernameContaining(username, pageable);
        } else if (action != null && !action.isEmpty()) {
            return auditLogRepository.findByActionContaining(action, pageable);
        }
        
        return auditLogRepository.findAll(pageable);
    }

    @Override
    public void deleteLogsInPeriod(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        auditLogRepository.deleteByTimestampBetween(start, end);
    }
}
