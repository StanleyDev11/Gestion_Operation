package com.ace3i.connect.service;

import com.ace3i.connect.entity.Mission;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Optional;

public interface IMissionService {
    List<Mission> getAllMissions();
    Page<Mission> getMissions(int page, int size, String keyword, String status);
    Optional<Mission> getMissionById(Long id);
    Mission saveMission(Mission mission);
    void deleteMission(Long id);
    byte[] generateMissionPdf(Mission mission) throws java.io.IOException;

    // Nouvelles méthodes pour le rapport périodique
    java.util.List<Mission> getMissionsByClientAndPeriod(String clientName, java.time.LocalDateTime start, java.time.LocalDateTime end);
    byte[] generatePeriodicReportPdf(String clientName, java.time.LocalDateTime start, java.time.LocalDateTime end, java.util.List<Mission> missions) throws java.io.IOException;
    }

