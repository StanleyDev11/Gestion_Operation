package com.ace3i.connect.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "CNDEPLOYMENT")
@Data
@NoArgsConstructor
public class ClientDeployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DEPID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private ApplicationVersion applicationVersion;

    @Column(name = "DEPDATE")
    private LocalDate deploymentDate;

    @Column(name = "DEPSTATUS")
    private String status; // e.g., "Active", "Retired"

    @OneToMany(mappedBy = "clientDeployment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.List<ClientDeploymentFeature> clientDeploymentFeatures;
}
