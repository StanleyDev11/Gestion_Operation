package com.ace3i.connect.service.impl;

import com.ace3i.connect.entity.*;
import com.ace3i.connect.repository.AppConfigRepository;
import com.ace3i.connect.repository.TicketAttachmentRepository;
import com.ace3i.connect.repository.TicketCommentRepository;
import com.ace3i.connect.repository.TicketRepository;
import com.ace3i.connect.service.IAuditLogService;
import com.ace3i.connect.service.IEmailService;
import com.ace3i.connect.service.INotificationService;
import com.ace3i.connect.service.ITicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class TicketService implements ITicketService {

    private final Path fileStorageLocation = Paths.get("uploads", "tickets").toAbsolutePath().normalize();

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketCommentRepository commentRepository;

    @Autowired
    private TicketAttachmentRepository attachmentRepository;

    @Autowired
    private AppConfigRepository configRepository;

    @Autowired
    private INotificationService notificationService;

    @Autowired
    private IEmailService emailService;

    @Autowired
    private IAuditLogService auditLogService;

    public TicketService() {
        try {
            Files.createDirectories(fileStorageLocation);
        } catch (Exception ex) {
            System.err.println("Impossible de créer le répertoire de stockage des pièces jointes : " + ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> getTicketsForClient(Client client, String status, String keyword, Pageable pageable) {
        String cleanStatus = (status != null && !status.trim().isEmpty()) ? status.trim() : null;
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return ticketRepository.searchByClient(client, cleanStatus, cleanKeyword, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Ticket> getTicketByIdAndClient(Long id, Client client) {
        return ticketRepository.findById(id)
                .filter(ticket -> ticket.getClient() != null && ticket.getClient().getId().equals(client.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getStatisticsForClient(Client client) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("nouveau", ticketRepository.countByClientAndStatus(client, "NOUVEAU"));
        stats.put("enCours", ticketRepository.countByClientAndStatus(client, "EN_COURS"));
        stats.put("enAttente", ticketRepository.countByClientAndStatus(client, "EN_ATTENTE"));
        stats.put("resolu", ticketRepository.countByClientAndStatus(client, "RESOLU"));
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> getAllTickets(Long clientId, String status, String urgency, String keyword, Pageable pageable) {
        String cleanStatus = (status != null && !status.trim().isEmpty()) ? status.trim() : null;
        String cleanUrgency = (urgency != null && !urgency.trim().isEmpty()) ? urgency.trim() : null;
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return ticketRepository.searchAll(clientId, cleanStatus, cleanUrgency, cleanKeyword, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Ticket> getTicketById(Long id) {
        return ticketRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getGlobalStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("nouveau", ticketRepository.countByStatus("NOUVEAU"));
        stats.put("enCours", ticketRepository.countByStatus("EN_COURS"));
        stats.put("enAttente", ticketRepository.countByStatus("EN_ATTENTE"));
        stats.put("resolu", ticketRepository.countByStatus("RESOLU"));
        stats.put("bloquante", ticketRepository.countByUrgency("BLOQUANTE"));
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardIncidentStats() {
        Map<String, Object> data = new HashMap<>();
        long bloquantesActives = ticketRepository.countByUrgencyAndStatusNotIn("BLOQUANTE", Arrays.asList("RESOLU", "CLOS"));
        long nouveaux = ticketRepository.countByStatus("NOUVEAU");
        long enCours = ticketRepository.countByStatus("EN_COURS");
        long resolus = ticketRepository.countByStatus("RESOLU");

        data.put("bloquantesActives", bloquantesActives);
        data.put("nouveaux", nouveaux);
        data.put("enCours", enCours);
        data.put("resolus", resolus);
        data.put("recentTickets", ticketRepository.findTop5ByOrderByCreatedAtDesc());

        // Groupements pour graphiques
        Map<String, Long> clientStats = new LinkedHashMap<>();
        for (Object[] row : ticketRepository.countByClientGrouped()) {
            if (row[0] != null && row[1] != null) {
                clientStats.put((String) row[0], ((Number) row[1]).longValue());
            }
        }
        data.put("byClient", clientStats);

        Map<String, Long> appStats = new LinkedHashMap<>();
        for (Object[] row : ticketRepository.countByApplicationGrouped()) {
            if (row[0] != null && row[1] != null) {
                appStats.put((String) row[0], ((Number) row[1]).longValue());
            }
        }
        data.put("byApp", appStats);

        return data;
    }

    @Override
    public Ticket createTicket(Ticket ticket, AppUser creator, List<MultipartFile> files) throws IOException {
        ticket.setCreatedBy(creator);
        ticket.setClient(creator.getClient());
        ticket.setStatus("NOUVEAU");
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        // Génération du numéro de ticket lisible
        Ticket lastTicket = ticketRepository.findTopByOrderByIdDesc();
        long nextId = (lastTicket != null && lastTicket.getId() != null) ? lastTicket.getId() + 1 : 1;
        String generatedNumber = String.format("INC-%d-%04d", LocalDate.now().getYear(), nextId);
        ticket.setTicketNumber(generatedNumber);

        Ticket savedTicket = ticketRepository.save(ticket);

        // Sauvegarde des pièces jointes si fournies
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    saveAttachment(savedTicket, file);
                }
            }
        }

        // Audit Log
        auditLogService.log("Création Incident",
                String.format("Incident %s créé par %s pour la banque %s (Urgence: %s)",
                        savedTicket.getTicketNumber(),
                        creator.getUsername(),
                        savedTicket.getClient().getName(),
                        savedTicket.getUrgency()));

        // Notification interne aux équipes ACE3i
        String notifTitle = String.format("Nouvel incident %s - %s", savedTicket.getTicketNumber(), savedTicket.getClient().getName());
        String notifMsg = String.format("Urgence: %s | Application: %s | Sujet: %s",
                savedTicket.getUrgency(), savedTicket.getApplicationName(), savedTicket.getTitle());
        String badgeType = "BLOQUANTE".equalsIgnoreCase(savedTicket.getUrgency()) ? "danger" : "info";
        notificationService.createNotification(notifTitle, notifMsg, badgeType, "/tickets/process/" + savedTicket.getId());

        // Alerte Email automatique si incident URGENT ou BLOQUANT
        if ("BLOQUANTE".equalsIgnoreCase(savedTicket.getUrgency()) || "URGENTE".equalsIgnoreCase(savedTicket.getUrgency())) {
            sendCriticalIncidentEmail(savedTicket);
        }

        return savedTicket;
    }

    private void saveAttachment(Ticket ticket, MultipartFile file) throws IOException {
        saveAttachment(ticket, null, file);
    }

    private TicketAttachment saveAttachment(Ticket ticket, TicketComment comment, MultipartFile file) throws IOException {
        String originalFileName = Paths.get(file.getOriginalFilename()).getFileName().toString();
        String fileExtension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = originalFileName.substring(dotIndex);
        }

        String storedFileName = UUID.randomUUID().toString() + fileExtension;
        Path targetLocation = this.fileStorageLocation.resolve(storedFileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        TicketAttachment attachment = new TicketAttachment();
        attachment.setTicket(ticket);
        attachment.setComment(comment);
        attachment.setFileName(originalFileName);
        attachment.setStoredFileName(storedFileName);
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setCreatedAt(LocalDateTime.now());

        return attachmentRepository.save(attachment);
    }

    private void sendCriticalIncidentEmail(Ticket ticket) {
        try {
            AppConfig config = configRepository.findById(1L).orElse(null);
            if (config != null && config.getNotificationEmail() != null && !config.getNotificationEmail().trim().isEmpty()) {
                String subject = String.format("🚨 [ALERTE INCIDENT %s] %s - %s",
                        ticket.getUrgency().toUpperCase(), ticket.getTicketNumber(), ticket.getClient().getName());

                String body = String.format(
                        "Bonjour l'équipe Support ACE3i,\n\n" +
                        "Un incident de niveau %s vient d'être déclaré sur le portail client :\n\n" +
                        "- Numéro : %s\n" +
                        "- Client (Banque) : %s\n" +
                        "- Demandeur : %s (%s)\n" +
                        "- Application : %s\n" +
                        "- Sujet : %s\n\n" +
                        "Description :\n%s\n\n" +
                        "Connectez-vous à ACE3i Connect pour prendre en charge cet incident immédiatement.",
                        ticket.getUrgency(),
                        ticket.getTicketNumber(),
                        ticket.getClient().getName(),
                        ticket.getCreatedBy().getFullName(),
                        ticket.getCreatedBy().getUsername(),
                        ticket.getApplicationName(),
                        ticket.getTitle(),
                        ticket.getDescription()
                );

                emailService.sendSimpleMessage(config.getNotificationEmail(), subject, body);
            }
        } catch (Exception e) {
            System.err.println("Échec d'envoi de l'email d'alerte incident : " + e.getMessage());
        }
    }

    @Override
    public Ticket updateTicketProcessing(Long ticketId, String status, String rootCause, String solutionProcedure, String assignedTo, AppUser user) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Incident non trouvé (ID: " + ticketId + ")"));

        String oldStatus = ticket.getStatus();
        ticket.setStatus(status);
        ticket.setRootCause(rootCause);
        ticket.setSolutionProcedure(solutionProcedure);
        ticket.setAssignedTo(assignedTo);
        ticket.setUpdatedAt(LocalDateTime.now());

        if ("RESOLU".equalsIgnoreCase(status) && !"RESOLU".equalsIgnoreCase(oldStatus)) {
            ticket.setResolvedAt(LocalDateTime.now());
            // Notification de résolution au client
            notificationService.createNotification(
                    "Incident Résolu : " + ticket.getTicketNumber(),
                    "Votre incident '" + ticket.getTitle() + "' a été résolu par l'équipe ACE3i.",
                    "success",
                    "/client/tickets/view/" + ticket.getId()
            );
        }

        Ticket saved = ticketRepository.save(ticket);
        auditLogService.log("Traitement Incident",
                String.format("Incident %s mis à jour par %s. Statut: %s, Intervenant: %s",
                        ticket.getTicketNumber(), user.getUsername(), status, assignedTo));

        return saved;
    }

    @Override
    public TicketComment addComment(Long ticketId, AppUser author, String content, boolean internalOnly) {
        try {
            return addComment(ticketId, author, content, internalOnly, null);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'ajout du commentaire", e);
        }
    }

    @Override
    public TicketComment addComment(Long ticketId, AppUser author, String content, boolean internalOnly, List<MultipartFile> files) throws IOException {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Incident non trouvé (ID: " + ticketId + ")"));

        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setContent(content);
        comment.setInternalOnly(internalOnly);
        comment.setCreatedAt(LocalDateTime.now());

        TicketComment saved = commentRepository.save(comment);

        // Sauvegarde des pièces jointes associées au commentaire
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    TicketAttachment att = saveAttachment(ticket, saved, file);
                    saved.getAttachments().add(att);
                }
            }
        }

        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        return saved;
    }

    @Override
    public TicketComment updateComment(Long commentId, String newContent, AppUser user) {
        TicketComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Commentaire non trouvé (ID: " + commentId + ")"));

        if (user == null || (!user.getId().equals(comment.getAuthor().getId()) && !"ROLE_ADMIN".equals(user.getRole()))) {
            throw new SecurityException("Vous n'êtes pas autorisé à modifier ce message.");
        }

        if (newContent == null || newContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Le contenu du message ne peut pas être vide.");
        }

        comment.setContent(newContent.trim());
        comment.setUpdatedAt(LocalDateTime.now());
        comment.setEdited(true);

        TicketComment updated = commentRepository.save(comment);

        auditLogService.log("Modification Message Ticket",
                String.format("Message #%d du ticket %s modifié par %s",
                        commentId, comment.getTicket().getTicketNumber(), user.getUsername()));

        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public TicketAttachment getAttachmentById(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Pièce jointe non trouvée (ID: " + attachmentId + ")"));
    }

    @Override
    public Path getAttachmentFilePath(TicketAttachment attachment) {
        return this.fileStorageLocation.resolve(attachment.getStoredFileName()).normalize();
    }
}
