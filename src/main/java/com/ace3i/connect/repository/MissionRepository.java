package com.ace3i.connect.repository;

import com.ace3i.connect.entity.Mission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MissionRepository extends JpaRepository<Mission, Long> {
    
    @Query("SELECT m FROM Mission m WHERE " +
           "(:keyword IS NULL OR LOWER(m.clientName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(m.intervener) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(m.projectName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:status IS NULL OR m.status = :status)")
    Page<Mission> searchMissions(@Param("keyword") String keyword, @Param("status") String status, Pageable pageable);
    @Query("SELECT m FROM Mission m WHERE m.clientName = :clientName AND m.startDate >= :start AND m.startDate <= :end ORDER BY m.startDate ASC")
    List<Mission> findByClientAndPeriod(@Param("clientName") String clientName, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT m FROM Mission m WHERE m.endDate < :now AND m.status != 'Terminée'")
    List<Mission> findOverdueMissions(@Param("now") LocalDateTime now);

    @Query("SELECT DISTINCT m.clientName FROM Mission m")
    List<String> findDistinctClientNames();
}
