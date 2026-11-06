package com.ace3i.connect.controller;

import com.ace3i.connect.entity.AppConfig;
import com.ace3i.connect.entity.Client;
import com.ace3i.connect.entity.Intervener;
import com.ace3i.connect.entity.TicketCategory;
import com.ace3i.connect.repository.AppConfigRepository;
import com.ace3i.connect.repository.ClientRepository;
import com.ace3i.connect.repository.IntervenerRepository;
import com.ace3i.connect.repository.TicketCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    @Autowired
    private AppConfigRepository configRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private IntervenerRepository intervenerRepository;

    @Autowired
    private TicketCategoryRepository ticketCategoryRepository;

    @GetMapping
    public String index() {
        return "redirect:/settings/general";
    }

    @GetMapping("/general")
    public String showGeneralSettings(Model model) {
        AppConfig config = configRepository.findById(1L).orElse(new AppConfig());
        model.addAttribute("config", config);
        return "settings-general";
    }

    @GetMapping("/banks")
    public String showBankSettings(Model model) {
        List<Client> allClients = clientRepository.findAll();
        
        // Séparer ACE3i des autres banques
        Client ace3i = allClients.stream()
                .filter(c -> "ACE3i".equalsIgnoreCase(c.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Client newAce = new Client();
                    newAce.setName("ACE3i");
                    return clientRepository.save(newAce);
                });
        
        List<Client> bankClients = allClients.stream()
                .filter(c -> !"ACE3i".equalsIgnoreCase(c.getName()))
                .collect(java.util.stream.Collectors.toList());

        model.addAttribute("clients", bankClients);
        model.addAttribute("ace3iClient", ace3i);
        model.addAttribute("newClient", new Client());
        model.addAttribute("newIntervener", new Intervener());
        return "settings-banks";
    }

    @PostMapping("/save")
    public String saveSettings(@ModelAttribute AppConfig updatedConfig,
                               @RequestParam(value = "logoLFile", required = false) MultipartFile logoLFile,
                               @RequestParam(value = "logoRFile", required = false) MultipartFile logoRFile,
                               @RequestParam(value = "watermarkFile", required = false) MultipartFile watermarkFile,
                               RedirectAttributes redirectAttributes) throws IOException {
        
        AppConfig config = configRepository.findById(1L).orElse(new AppConfig());
        
        config.setCompanyName(updatedConfig.getCompanyName());
        config.setAddress(updatedConfig.getAddress());
        config.setPhone(updatedConfig.getPhone());
        config.setEmail(updatedConfig.getEmail());

        if (logoLFile != null && !logoLFile.isEmpty()) {
            config.setLogoLeft("data:image/png;base64," + Base64.getEncoder().encodeToString(logoLFile.getBytes()));
        }
        if (logoRFile != null && !logoRFile.isEmpty()) {
            config.setLogoRight("data:image/png;base64," + Base64.getEncoder().encodeToString(logoRFile.getBytes()));
        }
        if (watermarkFile != null && !watermarkFile.isEmpty()) {
            config.setWatermarkImage("data:image/png;base64," + Base64.getEncoder().encodeToString(watermarkFile.getBytes()));
        }

        configRepository.save(config);
        redirectAttributes.addFlashAttribute("success", "Configuration mise à jour avec succès !");
        return "redirect:/settings/general";
    }

    @PostMapping("/clients/add")
    public String addClient(@ModelAttribute Client client, @RequestParam(required = false) String intervenerNames, RedirectAttributes redirectAttributes) {
        // Enregistrer d'abord le client pour avoir son ID
        Client savedClient = clientRepository.save(client);
        
        // Si des intervenants sont saisis
        if (intervenerNames != null && !intervenerNames.trim().isEmpty()) {
            String[] names = intervenerNames.split(",");
            for (String name : names) {
                if (!name.trim().isEmpty()) {
                    Intervener intervener = new Intervener();
                    intervener.setName(name.trim());
                    intervener.setClient(savedClient);
                    intervenerRepository.save(intervener);
                }
            }
        }
        
        redirectAttributes.addFlashAttribute("success", "Banque et intervenant(s) ajoutés avec succès !");
        return "redirect:/settings/banks";
    }

    @PostMapping("/clients/update-branch")
    public String updateClientBranch(@RequestParam Long clientId, @RequestParam(required = false) String gitBranch, RedirectAttributes redirectAttributes) {
        clientRepository.findById(clientId).ifPresent(client -> {
            client.setGitBranch(gitBranch != null && !gitBranch.isBlank() ? gitBranch.trim() : null);
            clientRepository.save(client);
            redirectAttributes.addFlashAttribute("success", "Branche Git mise à jour pour " + client.getName() + " !");
        });
        return "redirect:/settings/banks";
    }

    @PostMapping("/clients/delete/{id}")
    public String deleteClient(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        clientRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Client supprimé avec succès !");
        return "redirect:/settings/banks";
    }

    @GetMapping("/clients/delete-get/{id}")
    public String deleteClientGet(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return deleteClient(id, redirectAttributes);
    }

    @PostMapping("/interveners/add")
    public String addIntervener(@ModelAttribute Intervener intervener, @RequestParam Long clientId, RedirectAttributes redirectAttributes) {
        Client client = clientRepository.findById(clientId).orElse(null);
        if (client != null) {
            intervener.setClient(client);
            intervenerRepository.save(intervener);
            redirectAttributes.addFlashAttribute("success", "Intervenant ajouté avec succès !");
        } else {
            redirectAttributes.addFlashAttribute("error", "Client non trouvé !");
        }
        return "redirect:/settings/banks";
    }

    @PostMapping("/interveners/delete/{id}")
    public String deleteIntervener(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        intervenerRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Intervenant supprimé avec succès !");
        return "redirect:/settings/banks";
    }

    @GetMapping("/interveners/delete-get/{id}")
    public String deleteIntervenerGet(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return deleteIntervener(id, redirectAttributes);
    }

    // =====================================================================
    // GESTION DES CATEGORIES / MODULES DE TICKETS
    // =====================================================================

    @GetMapping("/ticket-categories")
    public String showTicketCategories(Model model) {
        model.addAttribute("categories", ticketCategoryRepository.findAllByOrderByDisplayOrderAscNameAsc());
        model.addAttribute("newCategory", new TicketCategory());
        return "settings-ticket-categories";
    }

    @PostMapping("/ticket-categories/add")
    public String addTicketCategory(@ModelAttribute TicketCategory category, RedirectAttributes redirectAttributes) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Le nom de la catégorie est obligatoire.");
            return "redirect:/settings/ticket-categories";
        }
        if (ticketCategoryRepository.existsByName(category.getName().trim())) {
            redirectAttributes.addFlashAttribute("error", "Une catégorie avec ce nom existe déjà.");
            return "redirect:/settings/ticket-categories";
        }
        category.setName(category.getName().trim());
        category.setActive(true);
        ticketCategoryRepository.save(category);
        redirectAttributes.addFlashAttribute("success", "Catégorie \"" + category.getName() + "\" ajoutée avec succès !");
        return "redirect:/settings/ticket-categories";
    }

    @PostMapping("/ticket-categories/toggle/{id}")
    public String toggleTicketCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ticketCategoryRepository.findById(id).ifPresent(cat -> {
            cat.setActive(!cat.isActive());
            ticketCategoryRepository.save(cat);
            redirectAttributes.addFlashAttribute("success",
                    "Catégorie \"" + cat.getName() + "\" " + (cat.isActive() ? "activée" : "désactivée") + ".");
        });
        return "redirect:/settings/ticket-categories";
    }

    @PostMapping("/ticket-categories/delete/{id}")
    public String deleteTicketCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ticketCategoryRepository.findById(id).ifPresent(cat -> {
            redirectAttributes.addFlashAttribute("success", "Catégorie \"" + cat.getName() + "\" supprimée.");
            ticketCategoryRepository.deleteById(id);
        });
        return "redirect:/settings/ticket-categories";
    }

    @GetMapping("/ticket-categories/delete-get/{id}")
    public String deleteTicketCategoryGet(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return deleteTicketCategory(id, redirectAttributes);
    }
}
