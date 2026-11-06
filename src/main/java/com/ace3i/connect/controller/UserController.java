package com.ace3i.connect.controller;

import com.ace3i.connect.entity.AppUser;
import com.ace3i.connect.repository.AppUserRepository;
import com.ace3i.connect.service.IAuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Controller
@RequestMapping("/admin/users")
public class UserController {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private com.ace3i.connect.repository.ClientRepository clientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private IAuditLogService auditLogService;

    @GetMapping
    public String listUsers(Model model,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String role) {
        
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String searchRole = (role != null && !role.trim().isEmpty()) ? role.trim() : null;

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<AppUser> userPage = userRepository.searchUsers(searchKeyword, searchRole, pageable);
        
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalItems", userPage.getTotalElements());
        model.addAttribute("keyword", searchKeyword);
        model.addAttribute("role", searchRole);
        model.addAttribute("size", size);
        
        return "user-list";
    }

    @GetMapping("/new")
    public String showForm(Model model) {
        model.addAttribute("user", new AppUser());
        model.addAttribute("clients", clientRepository.findAll());
        return "user-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        AppUser user = userRepository.findById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Utilisateur non trouvé.");
            return "redirect:/admin/users";
        }
        model.addAttribute("user", user);
        model.addAttribute("clients", clientRepository.findAll());
        return "user-form";
    }

    @PostMapping("/save")
    public String saveUser(@jakarta.validation.Valid @ModelAttribute("user") AppUser user, org.springframework.validation.BindingResult result, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "user-form";
        }

        boolean isNew = user.getId() == null;
        String action = isNew ? "Création Utilisateur" : "Modification Utilisateur";
        String details = "Nom d'utilisateur: " + user.getUsername() + ", Rôle: " + user.getRole();

        if (isNew) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            AppUser existing = userRepository.findById(user.getId()).orElse(null);
            if (existing != null) {
                if (user.getPassword() != null && !user.getPassword().isEmpty() && !user.getPassword().equals(existing.getPassword())) {
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                } else {
                    user.setPassword(existing.getPassword());
                }
            }
        }
        userRepository.save(user);
        auditLogService.log(action, details);
        redirectAttributes.addFlashAttribute("success", isNew ? "Utilisateur créé avec succès !" : "Utilisateur mis à jour avec succès !");
        return "redirect:/admin/users";
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        userRepository.findById(id).ifPresent(u -> {
            auditLogService.log("Suppression Utilisateur", "Suppression de " + u.getUsername() + " (ID: " + id + ")");
        });
        userRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "L'utilisateur a été supprimé.");
        return "redirect:/admin/users";
    }
}
