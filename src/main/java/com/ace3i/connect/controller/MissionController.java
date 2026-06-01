package com.ace3i.connect.controller;

import com.ace3i.connect.entity.Mission;
import com.ace3i.connect.service.IAuditLogService;
import com.ace3i.connect.service.IClientFeatureTrackingService;
import com.ace3i.connect.service.IMissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
public class MissionController {

    @Autowired
    private IMissionService missionService;

    @Autowired
    private IAuditLogService auditLogService;

    @Autowired
    private IClientFeatureTrackingService clientFeatureTrackingService;

    @Autowired
    private com.ace3i.connect.repository.ClientRepository clientRepository;

    @GetMapping("/")
    public String index(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            auditLogService.log("Accès Dashboard", "L'administrateur a accédé au tableau de bord.");
            return "redirect:/dashboard";
        }
        return "redirect:/missions";
    }

    @GetMapping("/missions")
    public String listMissions(Model model,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(required = false) String status) {
        
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword : null;
        String searchStatus = (status != null && !status.trim().isEmpty()) ? status : null;
        
        Page<Mission> missionPage = missionService.getMissions(page, size, searchKeyword, searchStatus);
        
        model.addAttribute("missions", missionPage.getContent());
        model.addAttribute("clients", clientRepository.findAll()); // Ajouté pour la modal de rapport
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", missionPage.getTotalPages());
        model.addAttribute("totalItems", missionPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("size", size);
        
        return "mission-list";
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model,
                                @RequestParam(required = false) String startDate,
                                @RequestParam(required = false) String endDate,
                                @RequestParam(required = false) String expert,
                                @RequestParam(required = false) String clientName) {
        
        List<Mission> allMissions = missionService.getAllMissions();
        List<Mission> filteredMissions = allMissions;

        // Filtrage simple en mémoire pour le dashboard
        if (startDate != null && !startDate.isEmpty()) {
            LocalDateTime start = java.time.LocalDate.parse(startDate).atStartOfDay();
            filteredMissions = filteredMissions.stream()
                    .filter(m -> m.getStartDate() != null && !m.getStartDate().isBefore(start))
                    .collect(Collectors.toList());
        }
        
        if (endDate != null && !endDate.isEmpty()) {
            LocalDateTime end = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);
            filteredMissions = filteredMissions.stream()
                    .filter(m -> m.getEndDate() != null && !m.getEndDate().isAfter(end))
                    .collect(Collectors.toList());
        }

        if (expert != null && !expert.isEmpty()) {
            filteredMissions = filteredMissions.stream()
                    .filter(m -> expert.equals(m.getIntervener()))
                    .collect(Collectors.toList());
        }

        if (clientName != null && !clientName.isEmpty()) {
            filteredMissions = filteredMissions.stream()
                    .filter(m -> m.getClientName() != null && m.getClientName().equals(clientName))
                    .collect(Collectors.toList());
        }

        // Préparer une liste simplifiée (DTO) pour le JavaScript
        List<Map<String, Object>> simplifiedMissions = filteredMissions.stream()
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("startDate", m.getStartDate() != null ? m.getStartDate().toString() : null);
                    map.put("endDate", m.getEndDate() != null ? m.getEndDate().toString() : null);
                    map.put("status", m.getStatus());
                    map.put("operationType", m.getOperationType());
                    map.put("intervener", m.getIntervener());
                    map.put("clientName", m.getClientName());
                    // Types de prestation
                    map.put("typeAnalyse", m.isTypeAnalyse());
                    map.put("typeFormation", m.isTypeFormation());
                    map.put("typeMaintenance", m.isTypeMaintenance());
                    map.put("typeProgrammation", m.isTypeProgrammation());
                    map.put("typeAssistance", m.isTypeAssistance());
                    map.put("typeInstallation", m.isTypeInstallation());
                    map.put("typeMiseEnProduction", m.isTypeMiseEnProduction());
                    map.put("typeSuiviProjet", m.isTypeSuiviProjet());
                    // Nouveaux types (Boolean wrapper)
                    map.put("typeCorrectif", m.getTypeCorrectif());
                    map.put("typeIncident", m.getTypeIncident());
                    map.put("typeSupport", m.getTypeSupport());
                    map.put("typeEvolution", m.getTypeEvolution());
                    return map;
                })
                .collect(Collectors.toList());

        model.addAttribute("missions", simplifiedMissions);
        model.addAttribute("rawMissionsCount", filteredMissions.size()); // Pour le badge total missions
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("expert", expert);
        model.addAttribute("selectedClientName", clientName);
        
        // Liste des experts pour le filtre
        List<String> experts = allMissions.stream()
                .map(Mission::getIntervener)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        model.addAttribute("expertsList", experts);

        // Liste de tous les clients pour le filtre
        model.addAttribute("allClients", clientRepository.findAll());

        // Resume global du suivi version/fonctionnalites
        model.addAttribute("featureGlobalKPIs", clientFeatureTrackingService.getGlobalKPIs());
        model.addAttribute("featureTotalCount", clientFeatureTrackingService.getAllFeatures().size());

        return "dashboard";
    }

    @GetMapping("/missions/calendar")
    public String showCalendar() {
        return "mission-calendar";
    }

    @GetMapping("/api/missions/events")
    @ResponseBody
    public List<Map<String, Object>> getCalendarEvents() {
        return missionService.getAllMissions().stream().map(m -> {
            Map<String, Object> event = new HashMap<>();
            event.put("id", m.getId().toString());
            event.put("title", m.getClientName() + " - " + m.getProjectName());
            event.put("start", m.getStartDate().toString());
            event.put("end", m.getEndDate().plusDays(1).toString()); // FullCalendar end date is exclusive
            
            // Couleur basée sur l'expert ou le statut
            String color = "#67bed9"; // Bleu ACE3i
            if ("Terminée".equals(m.getStatus())) color = "#28a745";
            if ("En cours".equals(m.getStatus())) color = "#ee7725";
            
            event.put("color", color);
            event.put("url", "/missions/edit/" + m.getId());
            
            Map<String, Object> extendedProps = new HashMap<>();
            extendedProps.put("expert", m.getIntervener());
            extendedProps.put("status", m.getStatus());
            event.put("extendedProps", extendedProps);
            
            return event;
        }).collect(Collectors.toList());
    }

    @GetMapping("/reports")
    public String showReportPage(Model model) {
        model.addAttribute("clients", clientRepository.findAll());
        return "report-form";
    }

    @GetMapping("/missions/new")
    public String showCreateForm(Model model) {
        model.addAttribute("mission", new Mission());
        
        List<com.ace3i.connect.entity.Client> allClients = clientRepository.findAll();
        model.addAttribute("clients", allClients.stream()
                .filter(c -> !"ACE3i".equalsIgnoreCase(c.getName()))
                .collect(Collectors.toList()));
        
        // Ajouter les experts ACE3i pour la sélection
        model.addAttribute("ace3iExperts", allClients.stream()
                .filter(c -> "ACE3i".equalsIgnoreCase(c.getName()))
                .findFirst()
                .map(com.ace3i.connect.entity.Client::getInterveners)
                .orElse(java.util.Collections.emptyList()));
                
        return "mission-form";
    }

    @GetMapping("/missions/edit/{id}")
    public String showEditForm(@PathVariable Long id, 
                               @RequestParam(required = false) String fKeyword,
                               @RequestParam(required = false) String fStatus,
                               @RequestParam(required = false) Integer fPage,
                               Model model) {
        missionService.getMissionById(id).ifPresent(mission -> model.addAttribute("mission", mission));
        
        List<com.ace3i.connect.entity.Client> allClients = clientRepository.findAll();
        model.addAttribute("clients", allClients.stream()
                .filter(c -> !"ACE3i".equalsIgnoreCase(c.getName()))
                .collect(Collectors.toList()));
        
        // Ajouter les experts ACE3i pour la sélection
        model.addAttribute("ace3iExperts", allClients.stream()
                .filter(c -> "ACE3i".equalsIgnoreCase(c.getName()))
                .findFirst()
                .map(com.ace3i.connect.entity.Client::getInterveners)
                .orElse(java.util.Collections.emptyList()));

        model.addAttribute("fKeyword", fKeyword);
        model.addAttribute("fStatus", fStatus);
        model.addAttribute("fPage", fPage);
        return "mission-form";
    }

    @PostMapping("/missions/save")
    public String saveMission(@jakarta.validation.Valid @ModelAttribute Mission mission, 
                               org.springframework.validation.BindingResult result, 
                               @RequestParam(required = false) String fKeyword,
                               @RequestParam(required = false) String fStatus,
                               @RequestParam(required = false) Integer fPage,
                               org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "mission-form";
        }
        
        if (mission.getStartDate() != null && mission.getEndDate() != null && mission.getEndDate().isBefore(mission.getStartDate())) {
            result.rejectValue("endDate", "error.mission", "La date de fin ne peut pas être antérieure à la date de début");
            return "mission-form";
        }

        boolean isNew = mission.getId() == null;
        String action = isNew ? "Création Mission" : "Modification Mission";
        String details = "Projet: " + mission.getProjectName() + " pour Client: " + mission.getClientName();
        missionService.saveMission(mission);
        auditLogService.log(action, details);
        
        redirectAttributes.addFlashAttribute("success", isNew ? "Mission créée avec succès !" : "Mission mise à jour avec succès !");
        
        // Redirection explicite vers la première page de la liste pour forcer le rafraîchissement
        if (isNew) {
            return "redirect:/missions?page=0&sort=startDate,desc";
        }

        // Redirection avec les paramètres de filtre conservés
        StringBuilder redirectUrl = new StringBuilder("redirect:/missions");
        boolean first = true;
        if (fPage != null) { redirectUrl.append(first ? "?" : "&").append("page=").append(fPage); first = false; }
        if (fKeyword != null && !fKeyword.isEmpty()) { redirectUrl.append(first ? "?" : "&").append("keyword=").append(fKeyword); first = false; }
        if (fStatus != null && !fStatus.isEmpty()) { redirectUrl.append(first ? "?" : "&").append("status=").append(fStatus); first = false; }
        
        return redirectUrl.toString();
    }

    @GetMapping("/missions/delete/{id}")
    public String deleteMission(@PathVariable Long id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        missionService.getMissionById(id).ifPresent(m -> {
            auditLogService.log("Suppression Mission", "Suppression de la mission " + m.getProjectName() + " (ID: " + id + ")");
        });
        missionService.deleteMission(id);
        redirectAttributes.addFlashAttribute("success", "La mission a été supprimée.");
        return "redirect:/missions";
    }

    @GetMapping("/missions/pdf/{id}")
    public ResponseEntity<byte[]> generateMissionPdf(@PathVariable Long id) {
        return missionService.getMissionById(id)
                .map(mission -> {
                    try {
                        byte[] pdfBytes = missionService.generateMissionPdf(mission);
                        return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Fiche_Intervention_" + mission.getClientName().replace(" ", "_") + ".pdf")
                                .contentType(MediaType.APPLICATION_PDF)
                                .body(pdfBytes);
                    } catch (IOException e) {
                        return ResponseEntity.internalServerError().<byte[]>build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/missions/report")
    public ResponseEntity<byte[]> getPeriodicReport(@RequestParam String clientName,
                                                   @RequestParam String startDate,
                                                   @RequestParam String endDate) {
        try {
            java.time.LocalDateTime start = java.time.LocalDate.parse(startDate).atStartOfDay();
            java.time.LocalDateTime end = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);
            java.util.List<Mission> missions = missionService.getMissionsByClientAndPeriod(clientName, start, end);
            byte[] pdfBytes = missionService.generatePeriodicReportPdf(clientName, start, end, missions);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Rapport_" + clientName.replace(" ", "_") + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
