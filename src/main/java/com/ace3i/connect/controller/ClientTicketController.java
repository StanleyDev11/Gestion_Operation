package com.ace3i.connect.controller;

import com.ace3i.connect.entity.*;
import com.ace3i.connect.repository.AppUserRepository;
import com.ace3i.connect.repository.TicketCategoryRepository;
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

@Controller
@RequestMapping("/client/tickets")
public class ClientTicketController {

    @Autowired
    private ITicketService ticketService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private TicketCommentRepository commentRepository;

    @Autowired
    private TicketCategoryRepository ticketCategoryRepository;

    private AppUser getAuthenticatedUser(Authentication authentication) {
        if (authentication == null) return null;
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }

    @GetMapping
    public String listClientTickets(Model model,
                                    Authentication authentication,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) String status) {

        AppUser user = getAuthenticatedUser(authentication);
        if (user == null || user.getClient() == null) {
            model.addAttribute("errorMessage", "Votre compte utilisateur n'est associé à aucune banque partenaire. Veuillez contacter l'administrateur ACE3i.");
            return "error";
        }

        Client client = user.getClient();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Ticket> ticketPage = ticketService.getTicketsForClient(client, status, keyword, pageable);

        model.addAttribute("tickets", ticketPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ticketPage.getTotalPages());
        model.addAttribute("totalItems", ticketPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("client", client);
        model.addAttribute("stats", ticketService.getStatisticsForClient(client));
        model.addAttribute("activePage", "client-tickets");

        return "client/ticket-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model, Authentication authentication) {
        AppUser user = getAuthenticatedUser(authentication);
        if (user == null || user.getClient() == null) {
            model.addAttribute("errorMessage", "Compte non rattaché à une banque.");
            return "error";
        }

        model.addAttribute("ticket", new Ticket());
        model.addAttribute("client", user.getClient());
        model.addAttribute("categories", ticketCategoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc());
        model.addAttribute("activePage", "client-tickets-new");
        return "client/ticket-create";
    }

    @PostMapping("/save")
    public String saveTicket(@ModelAttribute("ticket") Ticket ticket,
                             @RequestParam(value = "files", required = false) List<MultipartFile> files,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(authentication);
        if (user == null || user.getClient() == null) {
            redirectAttributes.addFlashAttribute("error", "Opération non autorisée.");
            return "redirect:/client/tickets";
        }

        try {
            Ticket saved = ticketService.createTicket(ticket, user, files);
            String typeLabel = "DEMANDE".equals(saved.getTicketType()) ? "Demande" : "Incident";
            redirectAttributes.addFlashAttribute("success",
                    typeLabel + " créé(e) avec succès sous la référence " + saved.getTicketNumber() + ".");
            return "redirect:/client/tickets/view/" + saved.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la déclaration de l'incident : " + e.getMessage());
            return "redirect:/client/tickets/new";
        }
    }

    @GetMapping("/view/{id}")
    public String viewTicket(@PathVariable Long id, Model model, Authentication authentication) {
        AppUser user = getAuthenticatedUser(authentication);
        if (user == null || user.getClient() == null) {
            return "redirect:/login";
        }

        Ticket ticket = ticketService.getTicketByIdAndClient(id, user.getClient()).orElse(null);
        if (ticket == null) {
            model.addAttribute("errorMessage", "Incident introuvable ou vous n'avez pas l'autorisation d'y accéder.");
            return "error";
        }

        model.addAttribute("ticket", ticket);
        model.addAttribute("currentUser", user);
        // Le client ne voit que les commentaires publics (internalOnly == false)
        model.addAttribute("comments", commentRepository.findByTicketAndInternalOnlyFalseOrderByCreatedAtAsc(ticket));
        model.addAttribute("activePage", "client-tickets");
        return "client/ticket-view";
    }

    @PostMapping("/{id}/comment")
    public String addComment(@PathVariable Long id,
                             @RequestParam("content") String content,
                             @RequestParam(value = "files", required = false) List<MultipartFile> files,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(authentication);
        if (user == null || user.getClient() == null) {
            return "redirect:/login";
        }

        Ticket ticket = ticketService.getTicketByIdAndClient(id, user.getClient()).orElse(null);
        if (ticket == null) {
            redirectAttributes.addFlashAttribute("error", "Incident introuvable.");
            return "redirect:/client/tickets";
        }

        if (content != null && !content.trim().isEmpty()) {
            try {
                ticketService.addComment(id, user, content.trim(), false, files);
                redirectAttributes.addFlashAttribute("success", "Message et pièce(s) jointe(s) transmis avec succès.");
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("error", "Erreur lors de l'envoi : " + e.getMessage());
            }
        }

        return "redirect:/client/tickets/view/" + id;
    }

    @PostMapping("/{ticketId}/comment/{commentId}/edit")
    public String editComment(@PathVariable Long ticketId,
                              @PathVariable Long commentId,
                              @RequestParam("content") String content,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(authentication);
        if (user == null || user.getClient() == null) {
            return "redirect:/login";
        }

        try {
            ticketService.updateComment(commentId, content, user);
            redirectAttributes.addFlashAttribute("success", "Votre message a été modifié.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Impossible de modifier le message : " + e.getMessage());
        }

        return "redirect:/client/tickets/view/" + ticketId;
    }

    @GetMapping("/attachment/{id}")
    @ResponseBody
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id, Authentication authentication) {
        AppUser user = getAuthenticatedUser(authentication);
        if (user == null || user.getClient() == null) {
            return ResponseEntity.status(403).build();
        }

        TicketAttachment attachment = ticketService.getAttachmentById(id);
        if (attachment == null || attachment.getTicket() == null ||
                !attachment.getTicket().getClient().getId().equals(user.getClient().getId())) {
            return ResponseEntity.status(403).build();
        }

        // Si la pièce jointe est attachée à une note interne, le client n'y a pas accès
        if (attachment.getComment() != null && attachment.getComment().isInternalOnly()) {
            return ResponseEntity.status(403).build();
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
}
