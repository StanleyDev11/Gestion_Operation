package com.ace3i.connect.scheduler;

import com.ace3i.connect.entity.AppConfig;
import com.ace3i.connect.entity.Mission;
import com.ace3i.connect.repository.AppConfigRepository;
import com.ace3i.connect.repository.MissionRepository;
import com.ace3i.connect.service.IEmailService;
import com.ace3i.connect.service.INotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class MissionAlertScheduler {

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private AppConfigRepository configRepository;

    @Autowired
    private INotificationService notificationService;

    @Autowired
    private IEmailService emailService;

    /**
     * Vérifie quotidiennement à 8h00 les missions qui ont dépassé leur date de fin.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void checkOverdueMissions() {
        List<Mission> overdueMissions = missionRepository.findOverdueMissions(LocalDateTime.now());
        AppConfig config = configRepository.findById(1L).orElse(null);
        
        for (Mission mission : overdueMissions) {
            String message = String.format("La mission '%s' pour le client %s aurait dû se terminer le %s.",
                    mission.getProjectName(), 
                    mission.getClientName(),
                    mission.getEndDate().toLocalDate().toString());
            
            // Notification Interne
            notificationService.createNotification(
                    "Alerte : Mission en retard",
                    message,
                    "warning",
                    "/missions/edit/" + mission.getId()
            );

            // Notification Email
            if (config != null && config.getNotificationEmail() != null) {
                emailService.sendSimpleMessage(
                        config.getNotificationEmail(),
                        "ACE3i Connect : Alerte Mission en retard",
                        message + "\n\nConsultez l'application pour plus de détails."
                );
            }
        }
    }

    /**
     * Rappel mensuel le 1er du mois à 9h00 pour générer les rapports périodiques.
     */
    @Scheduled(cron = "0 0 9 1 * *")
    public void remindMonthlyReports() {
        List<String> clients = missionRepository.findDistinctClientNames();
        AppConfig config = configRepository.findById(1L).orElse(null);
        
        if (!clients.isEmpty()) {
            String title = "Rappel : Rapports mensuels";
            String message = "C'est le début du mois ! N'oubliez pas de générer les rapports périodiques pour vos clients.";
            
            // Notification Interne
            notificationService.createNotification(title, message, "info", "/missions/reports");

            // Notification Email
            if (config != null && config.getNotificationEmail() != null) {
                emailService.sendSimpleMessage(
                        config.getNotificationEmail(),
                        "ACE3i Connect : " + title,
                        message
                );
            }
        }
    }
}
