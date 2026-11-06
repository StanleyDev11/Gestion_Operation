package com.ace3i.connect.controller;

import com.ace3i.connect.entity.*;
import com.ace3i.connect.repository.AppUserRepository;
import com.ace3i.connect.repository.ClientRepository;
import com.ace3i.connect.repository.TicketCommentRepository;
import com.ace3i.connect.service.ITicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/tickets")
public class TechnicianTicketController {

    @Autowired
    private ITicketService ticketService;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private TicketCommentRepository commentRepository;

    private AppUser getAuthenticatedUser(Authentication authentication) {
        if (authentication == null) return null;
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }

    @GetMapping
    public String listAllTickets(Model model,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 @RequestParam(required = false) Long clientId,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) String urgency,
                                 @RequestParam(required = false) String ticketType,
                                 @RequestParam(required = false) String keyword) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Ticket> ticketPage = ticketService.getAllTickets(clientId, status, urgency, keyword, pageable);

        // Filtrer par type si nécessaire (côté service ou ici)
        java.util.List<Ticket> filteredTickets = ticketPage.getContent();
        if (ticketType != null && !ticketType.isEmpty()) {
            filteredTickets = filteredTickets.stream()
                    .filter(t -> ticketType.equals(t.getTicketType()))
                    .collect(java.util.stream.Collectors.toList());
        }

        model.addAttribute("tickets", filteredTickets);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ticketPage.getTotalPages());
        model.addAttribute("totalItems", ticketPage.getTotalElements());
        model.addAttribute("clientId", clientId);
        model.addAttribute("status", status);
        model.addAttribute("urgency", urgency);
        model.addAttribute("ticketType", ticketType);
        model.addAttribute("keyword", keyword);
        model.addAttribute("clients", clientRepository.findAll());
        model.addAttribute("stats", ticketService.getGlobalStatistics());
        model.addAttribute("activePage", "tickets");

        return "ticket/ticket-dashboard";
    }

    @GetMapping("/process/{id}")
    public String showProcessView(@PathVariable Long id, Model model, Authentication authentication) {
        Ticket ticket = ticketService.getTicketById(id).orElse(null);
        if (ticket == null) {
            model.addAttribute("errorMessage", "Incident introuvable (ID: " + id + ")");
            return "error";
        }

        // Liste des techniciens ACE3i pour affectation
        List<AppUser> technicians = userRepository.findAll().stream()
                .filter(u -> "ROLE_TECHNICIAN".equals(u.getRole()) || "ROLE_ADMIN".equals(u.getRole()))
                .collect(Collectors.toList());

        model.addAttribute("ticket", ticket);
        model.addAttribute("currentUser", getAuthenticatedUser(authentication));
        model.addAttribute("technicians", technicians);
        // Le technicien voit TOUS les commentaires (publics + notes internes)
        model.addAttribute("comments", commentRepository.findByTicketOrderByCreatedAtAsc(ticket));
        model.addAttribute("activePage", "tickets");

        return "ticket/ticket-process";
    }

    @PostMapping("/process/{id}/save")
    public String saveTicketProcessing(@PathVariable Long id,
                                       @RequestParam("status") String status,
                                       @RequestParam(value = "rootCause", required = false) String rootCause,
                                       @RequestParam(value = "solutionProcedure", required = false) String solutionProcedure,
                                       @RequestParam(value = "assignedTo", required = false) String assignedTo,
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(authentication);
        try {
            ticketService.updateTicketProcessing(id, status, rootCause, solutionProcedure, assignedTo, user);
            redirectAttributes.addFlashAttribute("success", "L'incident a été mis à jour avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la mise à jour : " + e.getMessage());
        }

        return "redirect:/tickets/process/" + id;
    }

    @PostMapping("/process/{id}/comment")
    public String addStaffComment(@PathVariable Long id,
                                  @RequestParam("content") String content,
                                  @RequestParam(value = "internalOnly", defaultValue = "false") boolean internalOnly,
                                  @RequestParam(value = "files", required = false) List<MultipartFile> files,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(authentication);
        if (content != null && !content.trim().isEmpty()) {
            try {
                ticketService.addComment(id, user, content.trim(), internalOnly, files);
                redirectAttributes.addFlashAttribute("success", internalOnly ? "Note interne ajoutée avec succès." : "Message et fichier(s) transmis au client.");
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("error", "Erreur lors de l'enregistrement du message ou des fichiers : " + e.getMessage());
            }
        }
        return "redirect:/tickets/process/" + id;
    }

    @PostMapping("/process/{ticketId}/comment/{commentId}/edit")
    public String editStaffComment(@PathVariable Long ticketId,
                                   @PathVariable Long commentId,
                                   @RequestParam("content") String content,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(authentication);
        try {
            ticketService.updateComment(commentId, content, user);
            redirectAttributes.addFlashAttribute("success", "Message modifié avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Impossible de modifier le message : " + e.getMessage());
        }
        return "redirect:/tickets/process/" + ticketId;
    }

    @GetMapping("/attachment/{id}")
    @ResponseBody
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id) {
        TicketAttachment attachment = ticketService.getAttachmentById(id);
        if (attachment == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path filePath = ticketService.getAttachmentFilePath(attachment);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                String contentType = attachment.getFileType() != null ? attachment.getFileType() : "application/octet-stream";
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/convert-to-mission/{id}")
    public String convertToMission(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Ticket ticket = ticketService.getTicketById(id).orElse(null);
        if (ticket == null) {
            redirectAttributes.addFlashAttribute("error", "Incident introuvable.");
            return "redirect:/tickets";
        }

        // Pré-remplissage et redirection vers le formulaire de mission
        redirectAttributes.addFlashAttribute("prefillClient", ticket.getClient().getName());
        redirectAttributes.addFlashAttribute("prefillProject", ticket.getApplicationName());
        redirectAttributes.addFlashAttribute("prefillOrderNumber", ticket.getTicketNumber());
        redirectAttributes.addFlashAttribute("prefillIntervener", ticket.getAssignedTo());
        redirectAttributes.addFlashAttribute("prefillClientContact", ticket.getCreatedBy().getFullName());
        redirectAttributes.addFlashAttribute("prefillProblem", ticket.getDescription());
        redirectAttributes.addFlashAttribute("prefillRootCause", ticket.getRootCause());
        redirectAttributes.addFlashAttribute("prefillSolution", ticket.getSolutionProcedure());

        return "redirect:/missions/new";
    }
}
