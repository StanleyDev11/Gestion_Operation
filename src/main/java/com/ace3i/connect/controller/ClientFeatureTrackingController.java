package com.ace3i.connect.controller;

import com.ace3i.connect.entity.Client;
import com.ace3i.connect.repository.ApplicationVersionRepository;
import com.ace3i.connect.service.IClientFeatureTrackingService;
import com.ace3i.connect.repository.ClientRepository; // Assuming this already exists
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class ClientFeatureTrackingController {

    @Autowired
    private IClientFeatureTrackingService clientFeatureTrackingService;

    @Autowired
    private ClientRepository clientRepository; // To get all clients for dropdown

    @Autowired
    private ApplicationVersionRepository applicationVersionRepository; // To get all known versions

    @GetMapping("/feature-tracking/entry")
    public String showFeatureEntryForm(@RequestParam(required = false) Long clientId, Model model) {
        model.addAttribute("allClients", clientRepository.findAll());
        model.addAttribute("allApplicationVersions", applicationVersionRepository.findAll()); // Pass all versions for reference
        model.addAttribute("deploymentDate", LocalDate.now()); // Default to today
        model.addAttribute("selectedClientId", clientId);

        if (clientId != null) {
            clientFeatureTrackingService.getCurrentDeploymentForClient(clientId).ifPresent(deployment -> {
                model.addAttribute("selectedDeployment", deployment);
                model.addAttribute("selectedDeploymentFeatures", clientFeatureTrackingService.getDeploymentFeaturesForClient(clientId));
            });
        }
        return "feature-entry";
    }

    @PostMapping("/feature-tracking/save")
    public String saveFeatureDeployment(@RequestParam Long clientId,
                                        @RequestParam String versionNumber,
                                        @RequestParam(required = false) String projectName,
                                        @RequestParam(required = false) String branchName,
                                        @RequestParam String deploymentDate, // Received as String
                                        @RequestParam String deployedFeatures,
                                        @RequestParam(required = false) String clientSpecificFeatures,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        try {
            LocalDate parsedDeploymentDate = LocalDate.parse(deploymentDate);
            List<String> featureNames = Arrays.stream(deployedFeatures.split("\\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            List<String> specificFeatureNames = clientSpecificFeatures == null
                    ? List.of()
                    : Arrays.stream(clientSpecificFeatures.split("\\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            clientFeatureTrackingService.saveClientDeployment(clientId, versionNumber, projectName, branchName, parsedDeploymentDate, featureNames, specificFeatureNames);
            redirectAttributes.addFlashAttribute("success", "Déploiement client enregistré avec succès !");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'enregistrement: " + e.getMessage());
            // Re-add data to model for display if there's an error and we redirect back
            model.addAttribute("allClients", clientRepository.findAll());
            model.addAttribute("allApplicationVersions", applicationVersionRepository.findAll());
            model.addAttribute("selectedClientId", clientId);
            model.addAttribute("versionNumber", versionNumber);
            model.addAttribute("projectName", projectName);
            model.addAttribute("branchName", branchName);
            model.addAttribute("deploymentDate", deploymentDate);
            model.addAttribute("deployedFeatures", deployedFeatures);
            model.addAttribute("clientSpecificFeatures", clientSpecificFeatures);
            return "feature-entry"; // Return to form with error
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Une erreur inattendue est survenue: " + e.getMessage());
            // Similar re-adding of model attributes for other exceptions
            model.addAttribute("allClients", clientRepository.findAll());
            model.addAttribute("allApplicationVersions", applicationVersionRepository.findAll());
            model.addAttribute("selectedClientId", clientId);
            model.addAttribute("versionNumber", versionNumber);
            model.addAttribute("projectName", projectName);
            model.addAttribute("branchName", branchName);
            model.addAttribute("deploymentDate", deploymentDate);
            model.addAttribute("deployedFeatures", deployedFeatures);
            model.addAttribute("clientSpecificFeatures", clientSpecificFeatures);
            return "feature-entry"; // Return to form with error
        }
        return "redirect:/feature-tracking/dashboard"; // Redirect to dashboard after success
    }

    @GetMapping("/feature-tracking/dashboard")
    public String showFeatureDashboard(Model model) {
        List<com.ace3i.connect.entity.Client> allClients = clientRepository.findAll();
        
        // 1. Clients simplifiés
        List<Map<String, Object>> simplifiedClients = allClients.stream().map(c -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", c.getId());
            map.put("name", c.getName());
            return map;
        }).collect(Collectors.toList());
        model.addAttribute("allClients", simplifiedClients);

        // 2. Fonctionnalités simplifiées
        List<Map<String, Object>> simplifiedFeatures = clientFeatureTrackingService.getAllFeatures().stream().map(f -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", f.getId());
            map.put("name", f.getName());
            return map;
        }).collect(Collectors.toList());
        model.addAttribute("allFeatures", simplifiedFeatures);

        // 3. Déploiements simplifiés pour l'historique
        List<Map<String, Object>> simplifiedDeployments = clientFeatureTrackingService.getAllClientDeployments().stream()
                .map(d -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("clientName", d.getClient().getName());
                    map.put("versionNumber", d.getApplicationVersion().getVersionNumber());
                    map.put("deploymentDate", d.getDeploymentDate().toString());
                    map.put("status", d.getStatus());
                    return map;
                })
                .sorted((a, b) -> ((String)b.get("deploymentDate")).compareTo((String)a.get("deploymentDate")))
                .collect(Collectors.toList());
        model.addAttribute("allClientDeployments", simplifiedDeployments);

        // 4. Déploiements actuels par client (ID -> Version)
        Map<String, String> currentVersionByClient = new java.util.HashMap<>();
        for (com.ace3i.connect.entity.Client client : allClients) {
            clientFeatureTrackingService.getCurrentDeploymentForClient(client.getId())
                    .ifPresent(d -> currentVersionByClient.put(client.getId().toString(), d.getApplicationVersion().getVersionNumber()));
        }
        model.addAttribute("currentVersionByClient", currentVersionByClient);

        // 5. Statistiques de fonctionnalités par client (ID -> Map)
        Map<String, Map<String, List<String>>> allClientMissingAndNewFeatures = new java.util.HashMap<>();
        for (com.ace3i.connect.entity.Client client : allClients) {
            allClientMissingAndNewFeatures.put(client.getId().toString(), clientFeatureTrackingService.getMissingAndNewFeaturesForClient(client.getId()));
        }
        model.addAttribute("allClientMissingAndNewFeatures", allClientMissingAndNewFeatures);
        
        // 6. Autres données de reporting (Déjà simplifiées ou Maps de types simples)
        model.addAttribute("globalKPIs", clientFeatureTrackingService.getGlobalKPIs());
        
        // Conversion des scores de conformité (Long -> String pour Thymeleaf Map access)
        Map<String, Double> complianceScores = new java.util.HashMap<>();
        clientFeatureTrackingService.getClientComplianceScores().forEach((k, v) -> complianceScores.put(k.toString(), v));
        model.addAttribute("clientComplianceScores", complianceScores);

        // Matrice de fonctionnalités (ID Client -> Set ID Features)
        Map<String, java.util.Set<Long>> clientFeatureMap = new java.util.HashMap<>();
        clientFeatureTrackingService.getClientFeatureMap().forEach((k, v) -> clientFeatureMap.put(k.toString(), v));
        model.addAttribute("clientFeatureMap", clientFeatureMap);

        // Taux d'adoption par fonctionnalité (ID Feature -> Double)
        Map<String, Double> featureAdoptionRates = new java.util.HashMap<>();
        clientFeatureTrackingService.getFeatureAdoptionRates().forEach((k, v) -> featureAdoptionRates.put(k.toString(), v));
        model.addAttribute("featureAdoptionRates", featureAdoptionRates);

        // Versions d'application simplifiées
        List<Map<String, Object>> simplifiedVersions = clientFeatureTrackingService.getAllApplicationVersions().stream().map(v -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("versionNumber", v.getVersionNumber());
            map.put("releaseDate", v.getReleaseDate() != null ? v.getReleaseDate().toString() : "N/A");
            map.put("projectName", v.getProjectName());
            map.put("branchName", v.getBranchName());
            map.put("isLatestMaster", v.getIsLatestMaster());
            return map;
        }).collect(Collectors.toList());
        model.addAttribute("allApplicationVersions", simplifiedVersions);

        // 7. Données pour graphiques
        model.addAttribute("versionDistributionData", clientFeatureTrackingService.getVersionDistribution());
        model.addAttribute("featureStatusBreakdownData", clientFeatureTrackingService.getFeatureStatusBreakdown());
        model.addAttribute("clientFeatureAdoptionSummaryData", clientFeatureTrackingService.getClientFeatureAdoptionSummary());
        model.addAttribute("topMissingFeaturesData", clientFeatureTrackingService.getTopMissingFeatures(10));
        model.addAttribute("mostOutdatedClientsData", clientFeatureTrackingService.getMostOutdatedClients(10));
        model.addAttribute("versionAgeBucketsData", clientFeatureTrackingService.getVersionAgeBuckets());

        return "feature-dashboard";
    }

    @GetMapping("/feature-tracking/dashboard/print")
    public String printClientFeaturesByVersion(@RequestParam Long clientId,
                                               @RequestParam(required = false) Long deploymentId,
                                               Model model,
                                               RedirectAttributes redirectAttributes) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable."));

        List<com.ace3i.connect.entity.ClientDeployment> deployments = clientFeatureTrackingService.getDeploymentsForClient(clientId);
        if (deployments.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Ce client n'a aucune version déployée.");
            return "redirect:/feature-tracking/dashboard";
        }

        com.ace3i.connect.entity.ClientDeployment selectedDeployment;
        if (deploymentId == null) {
            selectedDeployment = deployments.get(0);
        } else {
            selectedDeployment = deployments.stream()
                    .filter(d -> d.getId().equals(deploymentId))
                    .findFirst()
                    .orElse(deployments.get(0));
        }

        model.addAttribute("client", client);
        model.addAttribute("deployments", deployments);
        model.addAttribute("selectedDeployment", selectedDeployment);
        model.addAttribute("selectedFeatures", clientFeatureTrackingService.getDeploymentFeaturesByDeploymentId(selectedDeployment.getId()));
        return "feature-print";
    }

    @GetMapping("/feature-tracking/set-master-version")
    public String showSetMasterVersionForm(Model model) {
        model.addAttribute("allApplicationVersions", clientFeatureTrackingService.getAllApplicationVersions());
        clientFeatureTrackingService.getLatestMasterVersion().ifPresent(
            master -> model.addAttribute("currentMasterVersion", master.getVersionNumber())
        );
        return "set-master-version";
    }

    @PostMapping("/feature-tracking/set-master-version")
    public String setMasterVersion(@RequestParam String versionNumber, RedirectAttributes redirectAttributes) {
        try {
            clientFeatureTrackingService.setLatestMasterVersion(versionNumber);
            redirectAttributes.addFlashAttribute("success", "Version maîtresse définie sur " + versionNumber + " avec succès !");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Une erreur inattendue est survenue: " + e.getMessage());
        }
        return "redirect:/feature-tracking/dashboard";
    }

    @PostMapping("/feature-tracking/feature/{deploymentFeatureId}/update")
    public String updateDeploymentFeature(@PathVariable Long deploymentFeatureId,
                                          @RequestParam String newFeatureName,
                                          @RequestParam Long clientId,
                                          RedirectAttributes redirectAttributes) {
        try {
            clientFeatureTrackingService.updateDeploymentFeature(deploymentFeatureId, newFeatureName);
            redirectAttributes.addFlashAttribute("success", "Fonctionnalite modifiee avec succes.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Une erreur inattendue est survenue: " + e.getMessage());
        }
        return "redirect:/feature-tracking/entry?clientId=" + clientId;
    }

    @PostMapping("/feature-tracking/feature/{deploymentFeatureId}/delete")
    public String deleteDeploymentFeature(@PathVariable Long deploymentFeatureId,
                                          @RequestParam Long clientId,
                                          RedirectAttributes redirectAttributes) {
        try {
            clientFeatureTrackingService.deleteDeploymentFeature(deploymentFeatureId);
            redirectAttributes.addFlashAttribute("success", "Fonctionnalite supprimee de la version client.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Une erreur inattendue est survenue: " + e.getMessage());
        }
        return "redirect:/feature-tracking/entry?clientId=" + clientId;
    }

    @PostMapping("/feature-tracking/feature/delete-selected")
    public String deleteSelectedDeploymentFeatures(@RequestParam(name = "deploymentFeatureIds", required = false) List<Long> deploymentFeatureIds,
                                                   @RequestParam Long clientId,
                                                   RedirectAttributes redirectAttributes) {
        try {
            clientFeatureTrackingService.deleteDeploymentFeatures(deploymentFeatureIds);
            redirectAttributes.addFlashAttribute("success", "Fonctionnalites selectionnees supprimees.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Une erreur inattendue est survenue: " + e.getMessage());
        }
        return "redirect:/feature-tracking/entry?clientId=" + clientId;
    }

    @PostMapping("/feature-tracking/clear-all")
    public String clearAllFeatureTrackingData(Authentication authentication,
                                              RedirectAttributes redirectAttributes) {
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!isAdmin) {
            redirectAttributes.addFlashAttribute("error", "Action non autorisee.");
            return "redirect:/feature-tracking/dashboard";
        }

        try {
            clientFeatureTrackingService.clearAllFeatureTrackingData();
            redirectAttributes.addFlashAttribute("success", "Toutes les donnees de suivi des fonctionnalites/version client ont ete supprimees.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Echec de suppression globale: " + e.getMessage());
        }
        return "redirect:/feature-tracking/dashboard";
    }
}
