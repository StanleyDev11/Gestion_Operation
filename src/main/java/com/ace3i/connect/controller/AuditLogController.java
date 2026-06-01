package com.ace3i.connect.controller;

import com.ace3i.connect.entity.AuditLog;
import com.ace3i.connect.repository.AppUserRepository;
import com.ace3i.connect.service.IAuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/logs")
public class AuditLogController {

    @Autowired
    private IAuditLogService auditLogService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public String showLogs(Model model, 
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "15") int size,
                           @RequestParam(required = false) String username,
                           @RequestParam(required = false) String action) {
        
        Page<AuditLog> logPage = auditLogService.getLogs(page, size, username, action);
        
        model.addAttribute("logs", logPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", logPage.getTotalPages());
        model.addAttribute("totalItems", logPage.getTotalElements());
        model.addAttribute("username", username);
        model.addAttribute("action", action);
        model.addAttribute("size", size);
        
        return "audit-logs";
    }

    @PostMapping("/purge")
    public String purgeLogs(@RequestParam String startDate,
                            @RequestParam String endDate,
                            @RequestParam String confirmPassword,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        
        if (!verifyAdminPassword(confirmPassword, authentication)) {
            redirectAttributes.addFlashAttribute("error", "Mot de passe de confirmation incorrect.");
            return "redirect:/admin/logs";
        }

        try {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
            
            auditLogService.deleteLogsInPeriod(start, end);
            auditLogService.log("Purge des Logs", "Suppression des journaux d'audit entre le " + startDate + " et le " + endDate);
            
            redirectAttributes.addFlashAttribute("success", "Les journaux d'audit ont été purgés avec succès pour la période sélectionnée.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la purge : " + e.getMessage());
        }

        return "redirect:/admin/logs";
    }

    private boolean verifyAdminPassword(String password, Authentication authentication) {
        if (authentication == null) return false;
        return userRepository.findByUsername(authentication.getName())
                .map(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElse(false);
    }
}
