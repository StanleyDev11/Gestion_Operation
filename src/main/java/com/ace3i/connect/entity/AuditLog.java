package com.ace3i.connect.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "CNAUD")
@Data
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AUDID")
    private Long id;

    @Column(name = "AUDNAM")
    private String username;

    @Column(name = "AUDACT")
    private String action; // Connexion, Création Mission, Suppression, etc.
    
    @Column(name = "AUDDET", columnDefinition = "TEXT")
    private String details; // Détails de l'action (ex: ID de la mission)
    
    @Column(name = "AUDTIM")
    private LocalDateTime timestamp;

    @Column(name = "AUDIP")
    private String ipAddress;

    public AuditLog(String username, String action, String details, String ipAddress) {
        this.username = username;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
        this.timestamp = LocalDateTime.now();
    }
}
