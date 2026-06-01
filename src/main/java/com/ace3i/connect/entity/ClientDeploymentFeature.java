package com.ace3i.connect.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CNDEPLOYMENT_FEATURE")
@Data
@NoArgsConstructor
public class ClientDeploymentFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CDFID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deployment_id", nullable = false)
    private ClientDeployment clientDeployment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id", nullable = false)
    private Feature feature;

    @Column(name = "CDFSTATUS")
    private String status; // e.g., "Deployed", "Configured", "Disabled"

    @Column(name = "CDFCLIENTSPECIFIC")
    private Boolean clientSpecific = false;
}
