package com.ace3i.connect.repository;

import com.ace3i.connect.entity.ClientDeploymentFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientDeploymentFeatureRepository extends JpaRepository<ClientDeploymentFeature, Long> {
    List<ClientDeploymentFeature> findByClientDeploymentId(Long clientDeploymentId);
}
