package com.ace3i.connect.service;

import com.ace3i.connect.entity.ApplicationVersion;
import com.ace3i.connect.entity.ClientDeployment;
import com.ace3i.connect.entity.ClientDeploymentFeature;
import com.ace3i.connect.entity.Feature;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface IClientFeatureTrackingService {

    ClientDeployment saveClientDeployment(Long clientId, String versionNumber, String projectName, String branchName, LocalDate deploymentDate, List<String> deployedFeatureNames, List<String> clientSpecificFeatureNames);

    void setLatestMasterVersion(String versionNumber);

    List<Feature> getDeployedFeaturesForClient(Long clientId);
    
    Map<String, List<String>> getMissingAndNewFeaturesForClient(Long clientId);

    List<ClientDeployment> getAllClientDeployments();

    // Methods for statistics/graphs
    Map<String, Long> getVersionDistribution();
    Map<String, Map<String, Long>> getFeatureStatusBreakdown();
    Map<String, Long> getClientFeatureAdoptionSummary();
    Map<String, Long> getTopMissingFeatures(int limit);
    Map<String, Object> getMostOutdatedClients(int limit);
    Map<String, Long> getVersionAgeBuckets();
    
    // New methods for a "Top" Dashboard
    Map<String, Object> getGlobalKPIs();
    Map<Long, Double> getClientComplianceScores();
    Map<Long, Set<Long>> getClientFeatureMap(); // New: ClientID -> Set of FeatureIDs
    Map<Long, Double> getFeatureAdoptionRates();

    List<Feature> getAllFeatures();
    List<ApplicationVersion> getAllApplicationVersions();
    Optional<ApplicationVersion> getLatestMasterVersion();
    Optional<ClientDeployment> getCurrentDeploymentForClient(Long clientId);
    List<ClientDeployment> getDeploymentsForClient(Long clientId);
    List<ClientDeploymentFeature> getDeploymentFeaturesForClient(Long clientId);
    List<ClientDeploymentFeature> getDeploymentFeaturesByDeploymentId(Long deploymentId);

    ApplicationVersion findOrCreateApplicationVersion(String versionNumber, String projectName, String branchName, LocalDate releaseDate, boolean isMaster);
    Feature findOrCreateFeature(String featureName);

    void updateDeploymentFeature(Long deploymentFeatureId, String newFeatureName);
    void deleteDeploymentFeature(Long deploymentFeatureId);
    void deleteDeploymentFeatures(List<Long> deploymentFeatureIds);
    void clearAllFeatureTrackingData();
}
