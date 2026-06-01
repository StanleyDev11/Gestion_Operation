package com.ace3i.connect.controller;

import com.ace3i.connect.repository.AppUserRepository;
import com.ace3i.connect.service.IAuditLogService;
import com.ace3i.connect.service.IDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/admin/database")
@PreAuthorize("hasRole('ADMIN')")
public class DatabaseController {

    @Autowired
    private IDatabaseService databaseService;

    @Autowired
    private IAuditLogService auditLogService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "database-management");
        model.addAttribute("pageTitle", "Gestion Base de Données");
        return "database-management";
    }

    @PostMapping("/export")
    public ResponseEntity<?> exportDatabase(@RequestParam("confirmPassword") String confirmPassword, Authentication authentication) {
        if (!verifyAdminPassword(confirmPassword, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Mot de passe incorrect.".getBytes());
        }

        String sql = databaseService.exportDatabase();
        byte[] data = sql.getBytes();

        String fileName = "backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".sql";

        auditLogService.log("Export Base de Données", "Exportation complète effectuée après vérification du mot de passe.");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @PostMapping("/import")
    public String importDatabase(@RequestParam("file") MultipartFile file, 
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (!verifyAdminPassword(confirmPassword, authentication)) {
            redirectAttributes.addFlashAttribute("error", "Mot de passe de confirmation incorrect.");
            return "redirect:/admin/database";
        }

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Veuillez sélectionner un fichier .sql.");
            return "redirect:/admin/database";
        }

        try {
            databaseService.importDatabase(file.getBytes());
            auditLogService.log("Import Base de Données", "Restauration effectuée après vérification du mot de passe avec le fichier : " + file.getOriginalFilename());
            redirectAttributes.addFlashAttribute("success", "La base de données a été restaurée avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'importation : " + e.getMessage());
        }

        return "redirect:/admin/database";
    }

    @PostMapping("/clear")
    public String clearDatabase(@RequestParam("confirmPassword") String confirmPassword,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (!verifyAdminPassword(confirmPassword, authentication)) {
            redirectAttributes.addFlashAttribute("error", "Mot de passe de confirmation incorrect.");
            return "redirect:/admin/database";
        }

        try {
            databaseService.clearDatabase();
            auditLogService.log("Remise à zéro Base de Données", "L'intégralité des données (hors comptes utilisateurs) a été supprimée.");
            redirectAttributes.addFlashAttribute("success", "La base de données a été vidée avec succès (les comptes utilisateurs ont été conservés).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la remise à zéro : " + e.getMessage());
        }

        return "redirect:/admin/database";
    }

    private boolean verifyAdminPassword(String password, Authentication authentication) {
        if (authentication == null) return false;
        return userRepository.findByUsername(authentication.getName())
                .map(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElse(false);
    }
}
