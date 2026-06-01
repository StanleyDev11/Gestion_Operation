package com.ace3i.connect.repository;

import com.ace3i.connect.entity.ClientDeployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientDeploymentRepository extends JpaRepository<ClientDeployment, Long> {
    List<ClientDeployment> findByClientId(Long clientId);
    List<ClientDeployment> findByClientIdOrderByDeploymentDateDesc(Long clientId);
    Optional<ClientDeployment> findTopByClientIdOrderByDeploymentDateDesc(Long clientId);
}
