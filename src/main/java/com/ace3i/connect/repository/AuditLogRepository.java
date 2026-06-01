package com.ace3i.connect.repository;

import com.ace3i.connect.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByTimestampDesc();
    
    Page<AuditLog> findByUsernameContaining(String username, Pageable pageable);
    Page<AuditLog> findByActionContaining(String action, Pageable pageable);
    Page<AuditLog> findByUsernameContainingAndActionContaining(String username, String action, Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM AuditLog l WHERE l.timestamp >= :start AND l.timestamp <= :end")
    void deleteByTimestampBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
