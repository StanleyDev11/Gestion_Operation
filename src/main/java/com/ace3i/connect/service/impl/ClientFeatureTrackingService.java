package com.ace3i.connect.service.impl;

import com.ace3i.connect.entity.*;
import com.ace3i.connect.repository.*;
import com.ace3i.connect.service.IClientFeatureTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClientFeatureTrackingService implements IClientFeatureTrackingService {

    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private ApplicationVersionRepository applicationVersionRepository;
    @Autowired
    private FeatureRepository featureRepository;
    @Autowired
    private ClientDeploymentRepository clientDeploymentRepository;
    @Autowired
    private ClientDeploymentFeatureRepository clientDeploymentFeatureRepository;

    @Override
    @Transactional
    public ClientDeployment saveClientDeployment(Long clientId, String versionNumber, String projectName, String branchName, LocalDate deploymentDate, List<String> deployedFeatureNames, List<String> clientSpecificFeatureNames) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found with ID: " + clientId));

        // Find or create ApplicationVersion with project and branch info
        ApplicationVersion applicationVersion = findOrCreateApplicationVersion(versionNumber, projectName, branchName, deploymentDate, false);

        // Create ClientDeployment
        ClientDeployment clientDeployment = new ClientDeployment();
        clientDeployment.setClient(client);
        clientDeployment.setApplicationVersion(applicationVersion);
        clientDeployment.setDeploymentDate(deploymentDate);
        clientDeployment.setStatus("Active"); // Default status

        clientDeployment = clientDeploymentRepository.save(clientDeployment);

        Set<String> globalNames = normalizeFeatureNames(deployedFeatureNames);
        Set<String> specificNames = normalizeFeatureNames(clientSpecificFeatureNames);
        specificNames.removeAll(globalNames);

        saveDeploymentFeatures(clientDeployment, globalNames, false);
        saveDeploymentFeatures(clientDeployment, specificNames, true);

        return clientDeployment;
    }

    @Override
    @Transactional
    public void setLatestMasterVersion(String versionNumber) {
        // Clear existing master flags
        applicationVersionRepository.findByIsLatestMasterTrue().ifPresent(oldMaster -> {
            oldMaster.setIsLatestMaster(false);
            applicationVersionRepository.save(oldMaster);
        });

        // Set new master flag
        ApplicationVersion newMaster = applicationVersionRepository.findByVersionNumber(versionNumber)
                .orElseThrow(() -> new IllegalArgumentException("Application Version not found: " + versionNumber));
        newMaster.setIsLatestMaster(true);
        applicationVersionRepository.save(newMaster);
    }

    @Override
    public List<Feature> getDeployedFeaturesForClient(Long clientId) {
        Optional<ClientDeployment> currentDeployment = getCurrentDeploymentForClient(clientId);
        if (currentDeployment.isEmpty()) {
            return Collections.emptyList();
        }
        return clientDeploymentFeatureRepository.findByClientDeploymentId(currentDeployment.get().getId()).stream()
                .map(ClientDeploymentFeature::getFeature)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, List<String>> getMissingAndNewFeaturesForClient(Long clientId) {
        Map<String, List<String>> comparisonResult = new HashMap<>();
        List<String> missingFeatures = new ArrayList<>();
        List<String> newFeatures = new ArrayList<>();

        Optional<ApplicationVersion> latestMasterVersionOpt = getLatestMasterVersion();
        Optional<ClientDeployment> currentClientDeploymentOpt = getCurrentDeploymentForClient(clientId);

        if (latestMasterVersionOpt.isEmpty() || currentClientDeploymentOpt.isEmpty()) {
            comparisonResult.put("missing", missingFeatures);
            comparisonResult.put("new", newFeatures);
            return comparisonResult;
        }

        ClientDeployment currentClientDeployment = currentClientDeploymentOpt.get();

        // Get features of the latest master version (reference)
        // This assumes features are directly linked to an ApplicationVersion if it's a master
        // For simplicity for now, let's assume 'latest master' holds ALL features known
        // A more robust solution might link features to specific versions.
        // For now, let's fetch all features from the DB as potential master features
        Set<String> masterFeatures = clientDeploymentFeatureRepository.findAll().stream()
                .filter(cdf -> !Boolean.TRUE.equals(cdf.getClientSpecific()))
                .map(cdf -> cdf.getFeature().getName())
                .collect(Collectors.toSet());
        if (masterFeatures.isEmpty()) {
            masterFeatures = featureRepository.findAll().stream()
                    .map(Feature::getName)
                    .collect(Collectors.toSet());
        }

        // Get features deployed for the current client
        List<ClientDeploymentFeature> clientFeatureLinks = clientDeploymentFeatureRepository
                .findByClientDeploymentId(currentClientDeployment.getId())
                .stream()
                .collect(Collectors.toList());
        Set<String> clientDeployedFeatures = clientFeatureLinks.stream()
                .map(cdf -> cdf.getFeature().getName())
                .collect(Collectors.toSet());

        // Features missing for the client (in master but not in client deployment)
        for (String masterFeature : masterFeatures) {
            if (!clientDeployedFeatures.contains(masterFeature)) {
                missingFeatures.add(masterFeature);
            }
        }

        // Features deployed by the client but not part of the current master (new/custom features)
        newFeatures = clientFeatureLinks.stream()
                .filter(cdf -> Boolean.TRUE.equals(cdf.getClientSpecific()))
                .map(cdf -> cdf.getFeature().getName())
                .distinct()
                .collect(Collectors.toList());

        comparisonResult.put("missing", missingFeatures);
        comparisonResult.put("new", newFeatures);
        return comparisonResult;
    }

    @Override
    public List<ClientDeployment> getAllClientDeployments() {
        return clientDeploymentRepository.findAll();
    }

    @Override
    public Map<String, Long> getVersionDistribution() {
        List<Client> allClients = clientRepository.findAll();
        Map<String, Long> distribution = new HashMap<>();
        for (Client client : allClients) {
            getCurrentDeploymentForClient(client.getId()).ifPresent(deployment -> {
                String version = deployment.getApplicationVersion().getVersionNumber();
                distribution.put(version, distribution.getOrDefault(version, 0L) + 1);
            });
        }
        return distribution;
    }

    @Override
    public Map<String, Map<String, Long>> getFeatureStatusBreakdown() {
        // This could be very complex, but for now, let's just return stats for all features in latest master
        Optional<ApplicationVersion> latestMasterOpt = getLatestMasterVersion();
        if (latestMasterOpt.isEmpty()) return Collections.emptyMap();
        
        // For each feature, count how many clients have it
        List<Feature> allFeatures = featureRepository.findAll();
        List<Client> allClients = clientRepository.findAll();
        Map<String, Long> breakdown = new HashMap<>();
        
        for (Feature feature : allFeatures) {
            long count = allClients.stream()
                    .map(c -> getCurrentDeploymentForClient(c.getId()))
                    .filter(Optional::isPresent)
                    .filter(d -> clientDeploymentFeatureRepository.findByClientDeploymentId(d.get().getId())
                            .stream().anyMatch(cdf -> cdf.getFeature().getId().equals(feature.getId())))
                    .count();
            breakdown.put(feature.getName(), count);
        }
        
        Map<String, Map<String, Long>> result = new HashMap<>();
        result.put("features", breakdown);
        return result;
    }

    @Override
    public Map<String, Long> getClientFeatureAdoptionSummary() {
        List<Client> allClients = clientRepository.findAll();
        Map<String, Long> summary = new HashMap<>();
        for (Client client : allClients) {
            long count = getDeployedFeaturesForClient(client.getId()).size();
            summary.put(client.getName(), count);
        }
        return summary;
    }

    @Override
    public Map<String, Long> getTopMissingFeatures(int limit) {
        Map<String, Long> counts = new HashMap<>();
        for (Client client : clientRepository.findAll()) {
            List<String> missing = getMissingAndNewFeaturesForClient(client.getId()).getOrDefault("missing", Collections.emptyList());
            for (String feature : missing) {
                counts.put(feature, counts.getOrDefault(feature, 0L) + 1);
            }
        }

        return counts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(Math.max(1, limit))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    @Override
    public Map<String, Object> getMostOutdatedClients(int limit) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Client client : clientRepository.findAll()) {
            Optional<ClientDeployment> deploymentOpt = getCurrentDeploymentForClient(client.getId());
            if (deploymentOpt.isEmpty()) {
                continue;
            }

            ClientDeployment deployment = deploymentOpt.get();
            long missingCount = getMissingAndNewFeaturesForClient(client.getId())
                    .getOrDefault("missing", Collections.emptyList())
                    .size();
            long ageDays = deployment.getDeploymentDate() == null
                    ? 0
                    : ChronoUnit.DAYS.between(deployment.getDeploymentDate(), LocalDate.now());

            Map<String, Object> row = new HashMap<>();
            row.put("clientName", client.getName());
            row.put("missingCount", missingCount);
            row.put("ageDays", ageDays);
            row.put("version", deployment.getApplicationVersion().getVersionNumber());
            rows.add(row);
        }

        rows.sort((a, b) -> {
            int cmpMissing = Long.compare(((Number) b.get("missingCount")).longValue(), ((Number) a.get("missingCount")).longValue());
            if (cmpMissing != 0) return cmpMissing;
            return Long.compare(((Number) b.get("ageDays")).longValue(), ((Number) a.get("ageDays")).longValue());
        });

        List<Map<String, Object>> top = rows.stream().limit(Math.max(1, limit)).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("rows", top);
        return result;
    }

    @Override
    public Map<String, Long> getVersionAgeBuckets() {
        Map<String, Long> buckets = new LinkedHashMap<>();
        buckets.put("0-30 jours", 0L);
        buckets.put("31-90 jours", 0L);
        buckets.put("90+ jours", 0L);

        for (Client client : clientRepository.findAll()) {
            Optional<ClientDeployment> deploymentOpt = getCurrentDeploymentForClient(client.getId());
            if (deploymentOpt.isEmpty() || deploymentOpt.get().getDeploymentDate() == null) {
                continue;
            }
            long age = ChronoUnit.DAYS.between(deploymentOpt.get().getDeploymentDate(), LocalDate.now());
            if (age <= 30) {
                buckets.put("0-30 jours", buckets.get("0-30 jours") + 1);
            } else if (age <= 90) {
                buckets.put("31-90 jours", buckets.get("31-90 jours") + 1);
            } else {
                buckets.put("90+ jours", buckets.get("90+ jours") + 1);
            }
        }
        return buckets;
    }

    @Override
    public Map<String, Object> getGlobalKPIs() {
        Map<String, Object> kpis = new HashMap<>();
        List<Client> allClients = clientRepository.findAll();
        Optional<ApplicationVersion> latestMasterOpt = getLatestMasterVersion();
        
        kpis.put("totalClients", (long) allClients.size());
        kpis.put("masterVersion", latestMasterOpt.map(ApplicationVersion::getVersionNumber).orElse("N/A"));
        
        if (latestMasterOpt.isPresent()) {
            String masterVersion = latestMasterOpt.get().getVersionNumber();
            long upToDateCount = allClients.stream()
                    .map(c -> getCurrentDeploymentForClient(c.getId()))
                    .filter(Optional::isPresent)
                    .filter(d -> d.get().getApplicationVersion().getVersionNumber().equals(masterVersion))
                    .count();
            kpis.put("upToDateClients", upToDateCount);
            
            // Calculate global adoption rate (average of all client adoption rates)
            double totalAdoption = 0;
            Map<Long, Double> scores = getClientComplianceScores();
            for (Double score : scores.values()) {
                totalAdoption += score;
            }
            double avgAdoption = allClients.isEmpty() ? 0 : totalAdoption / allClients.size();
            kpis.put("globalAdoptionRate", Math.round(avgAdoption * 10.0) / 10.0);
        } else {
            kpis.put("upToDateClients", 0L);
            kpis.put("globalAdoptionRate", 0.0);
        }
        
        return kpis;
    }

    @Override
    public Map<Long, Double> getClientComplianceScores() {
        List<Client> allClients = clientRepository.findAll();
        List<ClientDeploymentFeature> allLinks = clientDeploymentFeatureRepository.findAll();
        
        Set<Long> allMasterFeatureIds = allLinks.stream()
                .filter(cdf -> !Boolean.TRUE.equals(cdf.getClientSpecific()))
                .map(cdf -> cdf.getFeature().getId())
                .collect(Collectors.toSet());
                
        if (allMasterFeatureIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<Long, Double> scores = new HashMap<>();
        for (Client client : allClients) {
            // On cherche le déploiement actuel (le plus récent)
            Optional<ClientDeployment> currentDep = clientDeploymentRepository.findTopByClientIdOrderByDeploymentDateDesc(client.getId());
            if (currentDep.isPresent()) {
                Long depId = currentDep.get().getId();
                long clientMasterFeatureCount = allLinks.stream()
                    .filter(cdf -> cdf.getClientDeployment().getId().equals(depId))
                    .filter(cdf -> !Boolean.TRUE.equals(cdf.getClientSpecific()))
                    .map(cdf -> cdf.getFeature().getId())
                    .distinct()
                    .count();
                
                double score = (double) clientMasterFeatureCount / allMasterFeatureIds.size() * 100.0;
                scores.put(client.getId(), Math.round(score * 10.0) / 10.0);
            } else {
                scores.put(client.getId(), 0.0);
            }
        }
        return scores;
    }

    @Override
    public Map<Long, Set<Long>> getClientFeatureMap() {
        List<Client> allClients = clientRepository.findAll();
        List<ClientDeploymentFeature> allLinks = clientDeploymentFeatureRepository.findAll();
        
        Map<Long, Set<Long>> clientFeatureMap = new HashMap<>();
        for (Client client : allClients) {
            Optional<ClientDeployment> currentDep = clientDeploymentRepository.findTopByClientIdOrderByDeploymentDateDesc(client.getId());
            if (currentDep.isPresent()) {
                Long depId = currentDep.get().getId();
                Set<Long> featureIds = allLinks.stream()
                    .filter(cdf -> cdf.getClientDeployment().getId().equals(depId))
                    .map(cdf -> cdf.getFeature().getId())
                    .collect(Collectors.toSet());
                clientFeatureMap.put(client.getId(), featureIds);
            } else {
                clientFeatureMap.put(client.getId(), Collections.emptySet());
            }
        }
        return clientFeatureMap;
    }

    @Override
    public Map<Long, Double> getFeatureAdoptionRates() {
        List<Client> allClients = clientRepository.findAll();
        if (allClients.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> featureClientCount = new HashMap<>();
        for (Client client : allClients) {
            Set<Long> featureIds = getDeploymentFeaturesForClient(client.getId()).stream()
                    .filter(cdf -> !Boolean.TRUE.equals(cdf.getClientSpecific()))
                    .map(cdf -> cdf.getFeature().getId())
                    .collect(Collectors.toSet());
            for (Long featureId : featureIds) {
                featureClientCount.put(featureId, featureClientCount.getOrDefault(featureId, 0L) + 1);
            }
        }

        Map<Long, Double> rates = new HashMap<>();
        for (Map.Entry<Long, Long> entry : featureClientCount.entrySet()) {
            double pct = (double) entry.getValue() / allClients.size() * 100.0;
            rates.put(entry.getKey(), Math.round(pct * 10.0) / 10.0);
        }
        return rates;
    }

    @Override
    public List<Feature> getAllFeatures() {
        Set<Long> featureIds = clientDeploymentFeatureRepository.findAll().stream()
                .filter(cdf -> !Boolean.TRUE.equals(cdf.getClientSpecific()))
                .map(cdf -> cdf.getFeature().getId())
                .collect(Collectors.toSet());
        if (featureIds.isEmpty()) {
            return featureRepository.findAll();
        }
        return featureRepository.findAllById(featureIds);
    }

    @Override
    public List<ApplicationVersion> getAllApplicationVersions() {
        return applicationVersionRepository.findAll();
    }

    @Override
    public Optional<ApplicationVersion> getLatestMasterVersion() {
        return applicationVersionRepository.findByIsLatestMasterTrue();
    }

    @Override
    public Optional<ClientDeployment> getCurrentDeploymentForClient(Long clientId) {
        return clientDeploymentRepository.findTopByClientIdOrderByDeploymentDateDesc(clientId);
    }

    @Override
    public List<ClientDeployment> getDeploymentsForClient(Long clientId) {
        return clientDeploymentRepository.findByClientIdOrderByDeploymentDateDesc(clientId);
    }

    @Override
    public List<ClientDeploymentFeature> getDeploymentFeaturesForClient(Long clientId) {
        Optional<ClientDeployment> deploymentOpt = getCurrentDeploymentForClient(clientId);
        if (deploymentOpt.isEmpty()) {
            return Collections.emptyList();
        }
        return clientDeploymentFeatureRepository.findByClientDeploymentId(deploymentOpt.get().getId());
    }

    @Override
    public List<ClientDeploymentFeature> getDeploymentFeaturesByDeploymentId(Long deploymentId) {
        if (deploymentId == null) {
            return Collections.emptyList();
        }
        return clientDeploymentFeatureRepository.findByClientDeploymentId(deploymentId);
    }

    @Override
    @Transactional
    public ApplicationVersion findOrCreateApplicationVersion(String versionNumber, String projectName, String branchName, LocalDate releaseDate, boolean isMaster) {
        return applicationVersionRepository.findByVersionNumber(versionNumber)
                .map(existingVersion -> {
                    if (projectName != null && !projectName.isEmpty()) existingVersion.setProjectName(projectName);
                    if (branchName != null && !branchName.isEmpty()) existingVersion.setBranchName(branchName);
                    return applicationVersionRepository.save(existingVersion);
                })
                .orElseGet(() -> {
                    ApplicationVersion newVersion = new ApplicationVersion();
                    newVersion.setVersionNumber(versionNumber);
                    newVersion.setProjectName(projectName);
                    newVersion.setBranchName(branchName);
                    newVersion.setReleaseDate(releaseDate != null ? releaseDate : LocalDate.now());
                    newVersion.setIsLatestMaster(isMaster);
                    return applicationVersionRepository.save(newVersion);
                });
    }

    @Override
    @Transactional
    public Feature findOrCreateFeature(String featureName) {
        return featureRepository.findByName(featureName)
                .orElseGet(() -> {
                    Feature newFeature = new Feature();
                    newFeature.setName(featureName);
                    return featureRepository.save(newFeature);
                });
    }

    @Override
    @Transactional
    public void updateDeploymentFeature(Long deploymentFeatureId, String newFeatureName) {
        String normalizedName = newFeatureName == null ? "" : newFeatureName.trim();
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Le nom de la fonctionnalite ne peut pas etre vide.");
        }

        ClientDeploymentFeature deploymentFeature = clientDeploymentFeatureRepository.findById(deploymentFeatureId)
                .orElseThrow(() -> new IllegalArgumentException("Liaison fonctionnalite/deploiement introuvable."));

        Feature feature = findOrCreateFeature(normalizedName);
        deploymentFeature.setFeature(feature);
        clientDeploymentFeatureRepository.save(deploymentFeature);
    }

    @Override
    @Transactional
    public void deleteDeploymentFeature(Long deploymentFeatureId) {
        if (!clientDeploymentFeatureRepository.existsById(deploymentFeatureId)) {
            throw new IllegalArgumentException("Liaison fonctionnalite/deploiement introuvable.");
        }
        clientDeploymentFeatureRepository.deleteById(deploymentFeatureId);
    }

    @Override
    @Transactional
    public void deleteDeploymentFeatures(List<Long> deploymentFeatureIds) {
        if (deploymentFeatureIds == null || deploymentFeatureIds.isEmpty()) {
            throw new IllegalArgumentException("Aucune fonctionnalite selectionnee.");
        }

        for (Long id : deploymentFeatureIds) {
            if (id != null && clientDeploymentFeatureRepository.existsById(id)) {
                clientDeploymentFeatureRepository.deleteById(id);
            }
        }
    }

    @Override
    @Transactional
    public void clearAllFeatureTrackingData() {
        clientDeploymentFeatureRepository.deleteAllInBatch();
        clientDeploymentRepository.deleteAllInBatch();
        featureRepository.deleteAllInBatch();
        applicationVersionRepository.deleteAllInBatch();
    }

    private Set<String> normalizeFeatureNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return names.stream()
                .map(name -> name == null ? "" : name.trim())
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void saveDeploymentFeatures(ClientDeployment deployment, Set<String> featureNames, boolean isClientSpecific) {
        for (String featureName : featureNames) {
            Feature feature = findOrCreateFeature(featureName);
            ClientDeploymentFeature clientDeploymentFeature = new ClientDeploymentFeature();
            clientDeploymentFeature.setClientDeployment(deployment);
            clientDeploymentFeature.setFeature(feature);
            clientDeploymentFeature.setStatus("Deployed");
            clientDeploymentFeature.setClientSpecific(isClientSpecific);
            clientDeploymentFeatureRepository.save(clientDeploymentFeature);
        }
    }
}
