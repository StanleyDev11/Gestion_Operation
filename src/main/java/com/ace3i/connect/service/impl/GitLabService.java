package com.ace3i.connect.service.impl;

import com.ace3i.connect.entity.AppConfig;
import com.ace3i.connect.entity.ApplicationVersion;
import com.ace3i.connect.entity.Client;
import com.ace3i.connect.repository.ApplicationVersionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class GitLabService {

    private static final Logger log = LoggerFactory.getLogger(GitLabService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpClient createHttpClient() {
        try {
            // Support des certificats auto-signés pour GitLab interne auto-hébergé
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            return HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
        } catch (Exception e) {
            log.warn("Impossible de créer un client HTTP permissif SSL, repli sur client standard: {}", e.getMessage());
            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
        }
    }

    private String cleanBaseUrl(String url) {
        if (url == null) return "";
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String httpGet(String fullUrl, String token) throws Exception {
        HttpClient client = createHttpClient();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json");

        if (token != null && !token.isBlank()) {
            requestBuilder.header("PRIVATE-TOKEN", token.trim());
        }

        HttpRequest request = requestBuilder.GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return response.body();
        } else {
            throw new RuntimeException("Erreur GitLab HTTP " + status + ": " + response.body());
        }
    }

    /**
     * Teste la connexion au GitLab avec GET /api/v4/user ou /api/v4/version
     */
    public Map<String, Object> testConnection(String url, String token) {
        Map<String, Object> result = new HashMap<>();
        try {
            String baseUrl = cleanBaseUrl(url);
            if (baseUrl.isEmpty() || token == null || token.isBlank()) {
                result.put("success", false);
                result.put("message", "L'URL et le Token GitLab sont obligatoires pour tester la connexion.");
                return result;
            }

            // On teste avec /api/v4/user (ou si token de projet /api/v4/version)
            String targetUrl = baseUrl + "/api/v4/user";
            try {
                String responseBody = httpGet(targetUrl, token);
                Map<String, Object> user = objectMapper.readValue(responseBody, new TypeReference<>() {});
                String username = (String) user.getOrDefault("username", "Utilisateur GitLab");
                result.put("success", true);
                result.put("message", "Connexion réussie avec succès ! Authentifié en tant que : " + username);
                return result;
            } catch (Exception eUser) {
                // Si l'utilisateur n'a pas accès à /user (ex: Project Access Token), on tente /api/v4/projects
                String projectsUrl = baseUrl + "/api/v4/projects?per_page=1&membership=true";
                httpGet(projectsUrl, token);
                result.put("success", true);
                result.put("message", "Connexion à GitLab validée avec succès via Token de projet.");
                return result;
            }
        } catch (Exception e) {
            log.error("Échec du test de connexion GitLab: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "Échec de connexion : " + e.getMessage());
            return result;
        }
    }

    /**
     * Récupère la liste des branches d'un projet
     */
    public List<Map<String, Object>> getProjectBranches(String url, String token, String projectId) {
        try {
            String baseUrl = cleanBaseUrl(url);
            String encodedProjectId = URLEncoder.encode(projectId.trim(), StandardCharsets.UTF_8);
            String targetUrl = baseUrl + "/api/v4/projects/" + encodedProjectId + "/repository/branches?per_page=100";

            String json = httpGet(targetUrl, token);
            List<Map<String, Object>> rawBranches = objectMapper.readValue(json, new TypeReference<>() {});

            List<Map<String, Object>> branches = new ArrayList<>();
            for (Map<String, Object> rb : rawBranches) {
                Map<String, Object> b = new HashMap<>();
                b.put("name", rb.get("name"));
                Object commitObj = rb.get("commit");
                if (commitObj instanceof Map) {
                    Map<?, ?> commit = (Map<?, ?>) commitObj;
                    b.put("lastCommitId", commit.get("short_id"));
                    b.put("lastCommitDate", commit.get("committed_date"));
                    b.put("lastCommitAuthor", commit.get("author_name"));
                    b.put("lastCommitTitle", commit.get("title"));
                }
                branches.add(b);
            }
            return branches;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des branches pour le projet {}: {}", projectId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Récupère les infos d'une branche spécifique
     */
    public Map<String, Object> getBranchInfo(String url, String token, String projectId, String branchName) {
        try {
            String baseUrl = cleanBaseUrl(url);
            String encodedProjectId = URLEncoder.encode(projectId.trim(), StandardCharsets.UTF_8);
            String encodedBranch = URLEncoder.encode(branchName.trim(), StandardCharsets.UTF_8);
            String targetUrl = baseUrl + "/api/v4/projects/" + encodedProjectId + "/repository/branches/" + encodedBranch;

            String json = httpGet(targetUrl, token);
            Map<String, Object> raw = objectMapper.readValue(json, new TypeReference<>() {});

            Map<String, Object> info = new HashMap<>();
            info.put("name", raw.get("name"));
            Object commitObj = raw.get("commit");
            if (commitObj instanceof Map) {
                Map<?, ?> commit = (Map<?, ?>) commitObj;
                info.put("lastCommitId", commit.get("short_id"));
                info.put("fullCommitSha", commit.get("id"));
                info.put("lastCommitDate", commit.get("committed_date"));
                info.put("lastCommitAuthor", commit.get("author_name"));
                info.put("lastCommitTitle", commit.get("title"));
            }
            return info;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la branche {} : {}", branchName, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Récupère le dernier tag associé à une branche ou commit
     */
    public String getLatestTagForBranch(String url, String token, String projectId, String commitSha) {
        try {
            String baseUrl = cleanBaseUrl(url);
            String encodedProjectId = URLEncoder.encode(projectId.trim(), StandardCharsets.UTF_8);
            String targetUrl = baseUrl + "/api/v4/projects/" + encodedProjectId + "/repository/tags?per_page=50";

            String json = httpGet(targetUrl, token);
            List<Map<String, Object>> tags = objectMapper.readValue(json, new TypeReference<>() {});

            if (tags != null && !tags.isEmpty()) {
                // Si un commitSha est fourni, voir si un tag pointe directement dessus
                if (commitSha != null && !commitSha.isBlank()) {
                    for (Map<String, Object> t : tags) {
                        Object commitObj = t.get("commit");
                        if (commitObj instanceof Map) {
                            Map<?, ?> commit = (Map<?, ?>) commitObj;
                            String cId = (String) commit.get("id");
                            String shortId = (String) commit.get("short_id");
                            if (commitSha.equalsIgnoreCase(cId) || commitSha.equalsIgnoreCase(shortId)) {
                                return (String) t.get("name");
                            }
                        }
                    }
                }
                // Par défaut, retourner le tag le plus récent
                return (String) tags.get(0).get("name");
            }
        } catch (Exception e) {
            log.warn("Impossible de récupérer les tags GitLab: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Récupère le nom d'un projet GitLab
     */
    public String getProjectName(String url, String token, String projectId) {
        try {
            String baseUrl = cleanBaseUrl(url);
            String encodedProjectId = URLEncoder.encode(projectId.trim(), StandardCharsets.UTF_8);
            String targetUrl = baseUrl + "/api/v4/projects/" + encodedProjectId;
            String json = httpGet(targetUrl, token);
            Map<String, Object> p = objectMapper.readValue(json, new TypeReference<>() {});
            return (String) p.getOrDefault("name", projectId);
        } catch (Exception e) {
            log.warn("Impossible de récupérer le nom du projet {}: {}", projectId, e.getMessage());
            return projectId;
        }
    }

    /**
     * Synchronise une banque spécifique avec sa branche sur l'ensemble des projets GitLab configurés
     */
    public Map<String, Object> syncClientBranch(Client client, AppConfig config, ApplicationVersionRepository versionRepository) {
        Map<String, Object> result = new HashMap<>();
        result.put("clientName", client.getName());
        result.put("clientId", client.getId());

        if (client.getGitBranch() == null || client.getGitBranch().isBlank()) {
            result.put("success", false);
            result.put("message", "Aucune branche Git associée à cette banque.");
            return result;
        }

        String branchName = client.getGitBranch().trim();
        result.put("branchName", branchName);

        if (config.getGitlabUrl() == null || config.getGitlabUrl().isBlank()
                || config.getGitlabToken() == null || config.getGitlabToken().isBlank()
                || config.getGitlabProjectId() == null || config.getGitlabProjectId().isBlank()) {
            result.put("success", false);
            result.put("message", "Configuration GitLab incomplète (URL, Token ou ID(s) de projet manquant(s)).");
            return result;
        }

        // Support de plusieurs IDs de projets séparés par des virgules (ex: "22, 25, 30")
        String[] projectIds = config.getGitlabProjectId().split(",");
        List<Map<String, Object>> projectSyncDetails = new ArrayList<>();
        int syncedProjectsCount = 0;

        try {
            for (String rawPid : projectIds) {
                String pid = rawPid.trim();
                if (pid.isEmpty()) continue;

                String projectName = getProjectName(config.getGitlabUrl(), config.getGitlabToken(), pid);
                Map<String, Object> branchInfo = getBranchInfo(config.getGitlabUrl(), config.getGitlabToken(), pid, branchName);

                if (branchInfo.isEmpty()) {
                    // La branche n'existe pas dans ce projet précis, on continue
                    continue;
                }

                String shortCommitId = (String) branchInfo.getOrDefault("lastCommitId", "unknown");
                String fullCommitSha = (String) branchInfo.get("fullCommitSha");
                String commitDateStr = (String) branchInfo.get("lastCommitDate");
                String commitAuthor = (String) branchInfo.get("lastCommitAuthor");

                LocalDate releaseDate = LocalDate.now();
                if (commitDateStr != null) {
                    try {
                        releaseDate = ZonedDateTime.parse(commitDateStr).toLocalDate();
                    } catch (Exception pe) {
                        log.warn("Date commit non parsable {}, fallback to now: {}", commitDateStr, pe.getMessage());
                    }
                }

                String tag = getLatestTagForBranch(config.getGitlabUrl(), config.getGitlabToken(), pid, fullCommitSha);
                String computedVersion = (tag != null && !tag.isBlank()) ? tag : (branchName + "-" + shortCommitId);

                // Chercher dans ApplicationVersion si cette branche pour ce projet existe déjà
                List<ApplicationVersion> allVersions = versionRepository.findAll();
                Optional<ApplicationVersion> existingOpt = allVersions.stream()
                        .filter(v -> branchName.equalsIgnoreCase(v.getBranchName()) &&
                                (projectName.equalsIgnoreCase(v.getProjectName()) || client.getName().equalsIgnoreCase(v.getProjectName())))
                        .findFirst();

                ApplicationVersion version;
                if (existingOpt.isPresent()) {
                    version = existingOpt.get();
                    version.setReleaseDate(releaseDate);
                    version.setBranchName(branchName);
                    version.setProjectName(projectName);
                    if (tag != null && !tag.isBlank()) {
                        version.setVersionNumber(tag);
                    }
                    version.setDescription("Mise à jour automatique GitLab (Projet: " + projectName + ", Commit: " + shortCommitId + " par " + commitAuthor + ")");
                } else {
                    version = new ApplicationVersion();
                    version.setVersionNumber(computedVersion);
                    version.setBranchName(branchName);
                    version.setProjectName(projectName);
                    version.setReleaseDate(releaseDate);
                    version.setIsLatestMaster(false);
                    version.setDescription("Création automatique via synchro GitLab (Projet: " + projectName + ", Commit: " + shortCommitId + ")");
                }

                versionRepository.save(version);
                syncedProjectsCount++;

                Map<String, Object> pDetail = new HashMap<>();
                pDetail.put("projectId", pid);
                pDetail.put("projectName", projectName);
                pDetail.put("versionNumber", version.getVersionNumber());
                pDetail.put("commitSha", shortCommitId);
                projectSyncDetails.add(pDetail);
            }

            if (syncedProjectsCount == 0) {
                result.put("success", false);
                result.put("message", "Branche '" + branchName + "' introuvable dans le(s) projet(s) configuré(s).");
                return result;
            }

            result.put("success", true);
            result.put("syncedCount", syncedProjectsCount);
            result.put("projects", projectSyncDetails);
            result.put("message", "Synchronisé avec succès sur " + syncedProjectsCount + " projet(s) (" + branchName + ").");
            return result;

        } catch (Exception e) {
            log.error("Erreur de synchronisation pour le client {}: {}", client.getName(), e.getMessage());
            result.put("success", false);
            result.put("message", "Erreur lors de la synchronisation : " + e.getMessage());
            return result;
        }
    }
}
