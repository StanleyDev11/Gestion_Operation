package com.ace3i.connect.service;

import com.ace3i.connect.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ITicketService {

    // Espace Client
    Page<Ticket> getTicketsForClient(Client client, String status, String keyword, Pageable pageable);
    Optional<Ticket> getTicketByIdAndClient(Long id, Client client);
    Map<String, Long> getStatisticsForClient(Client client);

    // Espace Technicien / Admin
    Page<Ticket> getAllTickets(Long clientId, String status, String urgency, String keyword, Pageable pageable);
    Optional<Ticket> getTicketById(Long id);
    Map<String, Long> getGlobalStatistics();
    Map<String, Object> getDashboardIncidentStats();

    // Actions métier
    Ticket createTicket(Ticket ticket, AppUser creator, List<MultipartFile> files) throws IOException;
    Ticket updateTicketProcessing(Long ticketId, String status, String rootCause, String solutionProcedure, String assignedTo, AppUser user);
    TicketComment addComment(Long ticketId, AppUser author, String content, boolean internalOnly);
    TicketComment addComment(Long ticketId, AppUser author, String content, boolean internalOnly, List<MultipartFile> files) throws IOException;
    TicketComment updateComment(Long commentId, String newContent, AppUser user);
    
    // Pièces jointes
    TicketAttachment getAttachmentById(Long attachmentId);
    Path getAttachmentFilePath(TicketAttachment attachment);
}
