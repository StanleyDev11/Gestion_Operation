package com.ace3i.connect.controller;

import com.ace3i.connect.entity.AppConfig;
import com.ace3i.connect.entity.Client;
import com.ace3i.connect.repository.AppConfigRepository;
import com.ace3i.connect.repository.ApplicationVersionRepository;
import com.ace3i.connect.repository.ClientRepository;
import com.ace3i.connect.service.impl.GitLabService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/settings/gitlab")
public class GitLabController {

    private static final Logger log = LoggerFactory.getLogger(GitLabController.class);

    @Autowired
    private AppConfigRepository configRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ApplicationVersionRepository versionRepository;

    @Autowired
    private GitLabService gitLabService;

    /**
     * Sauvegarde des paramètres de connexion GitLab depuis la page de configuration générale
     */
    @PostMapping("/save")
    public String saveGitlabSettings(@ModelAttribute AppConfig submittedConfig, RedirectAttributes redirectAttributes) {
        AppConfig config = configRepository.findById(1L).orElse(new AppConfig());

        config.setGitlabUrl(submittedConfig.getGitlabUrl());
        config.setGitlabToken(submittedConfig.getGitlabToken());
        config.setGitlabProjectId(submittedConfig.getGitlabProjectId());
        config.setGitlabDefaultBranch(submittedConfig.getGitlabDefaultBranch());

        configRepository.save(config);
        redirectAttributes.addFlashAttribute("success", "Configuration GitLab enregistrée avec succès !");
        return "redirect:/settings/general";
    }

    /**
     * Test de connexion en AJAX
     */
    @PostMapping("/test-connection")
    @ResponseBody
    public Map<String, Object> testConnection(
            @RequestParam(required = false) String gitlabUrl,
            @RequestParam(required = false) String gitlabToken) {

        AppConfig config = configRepository.findById(1L).orElse(new AppConfig());
        String url = (gitlabUrl != null && !gitlabUrl.isBlank()) ? gitlabUrl : config.getGitlabUrl();
        String token = (gitlabToken != null && !gitlabToken.isBlank()) ? gitlabToken : config.getGitlabToken();

        return gitLabService.testConnection(url, token);
    }

    /**
     * Synchronise toutes les banques ayant une branche Git configurée
     */
    @PostMapping("/sync")
    @ResponseBody
    public Map<String, Object> syncAll() {
        Map<String, Object> response = new HashMap<>();
        AppConfig config = configRepository.findById(1L).orElse(new AppConfig());

        List<Client> clients = clientRepository.findAll();
        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;
        int totalClientsWithBranch = 0;

        for (Client client : clients) {
            if (client.getGitBranch() != null && !client.getGitBranch().isBlank()) {
                totalClientsWithBranch++;
                Map<String, Object> syncRes = gitLabService.syncClientBranch(client, config, versionRepository);
                results.add(syncRes);
                if (Boolean.TRUE.equals(syncRes.get("success"))) {
                    successCount++;
                }
            }
        }

        response.put("success", true);
        response.put("totalChecked", totalClientsWithBranch);
        response.put("successCount", successCount);
        response.put("results", results);
        response.put("message", "Synchronisation terminée : " + successCount + " banque(s) mise(s) à jour sur " + totalClientsWithBranch + ".");

        return response;
    }

    /**
     * Synchronise une banque spécifique
     */
    @PostMapping("/sync/{clientId}")
    @ResponseBody
    public Map<String, Object> syncOne(@PathVariable Long clientId) {
        Optional<Client> clientOpt = clientRepository.findById(clientId);
        if (clientOpt.isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Banque introuvable (ID: " + clientId + ").");
            return err;
        }

        AppConfig config = configRepository.findById(1L).orElse(new AppConfig());
        return gitLabService.syncClientBranch(clientOpt.get(), config, versionRepository);
    }

    /**
     * Liste des branches du projet GitLab configuré
     */
    @GetMapping("/branches")
    @ResponseBody
    public Map<String, Object> getBranches() {
        Map<String, Object> response = new HashMap<>();
        AppConfig config = configRepository.findById(1L).orElse(new AppConfig());

        if (config.getGitlabUrl() == null || config.getGitlabToken() == null || config.getGitlabProjectId() == null) {
            response.put("success", false);
            response.put("branches", Collections.emptyList());
            response.put("message", "GitLab n'est pas configuré.");
            return response;
        }

        List<Map<String, Object>> branches = gitLabService.getProjectBranches(
                config.getGitlabUrl(),
                config.getGitlabToken(),
                config.getGitlabProjectId()
        );

        response.put("success", true);
        response.put("branches", branches);
        return response;
    }
}
