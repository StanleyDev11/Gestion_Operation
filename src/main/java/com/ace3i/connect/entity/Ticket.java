package com.ace3i.connect.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "CNTKT")
@Data
@NoArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TKTID")
    private Long id;

    @Column(name = "TKTNUM", unique = true, nullable = false, length = 30)
    private String ticketNumber; // Ex: INC-2026-0001

    @NotBlank(message = "Le titre de l'incident est obligatoire")
    @Column(name = "TKTTIT", nullable = false)
    private String title;

    @NotBlank(message = "L'application ou module concerné est obligatoire")
    @Column(name = "TKTAPP", nullable = false)
    private String applicationName; // Ex: Monétique, Virement, Portails, Core Banking, etc.

    @NotNull(message = "L'urgence est obligatoire")
    @Column(name = "TKTURG", nullable = false, length = 20)
    private String urgency = "NORMALE"; // FAIBLE, NORMALE, URGENTE, BLOQUANTE

    @Column(name = "TKTTYP", nullable = false, length = 20)
    private String ticketType = "INCIDENT"; // INCIDENT ou DEMANDE

    @Column(name = "TKTSTA", nullable = false, length = 20)
    private String status = "NOUVEAU"; // NOUVEAU, EN_COURS, EN_ATTENTE, RESOLU, CLOS

    @NotBlank(message = "La description du problème est obligatoire")
    @Column(name = "TKTDES", columnDefinition = "TEXT")
    private String description;

    @Column(name = "TKTCAU", columnDefinition = "TEXT")
    private String rootCause; // Cause racine identifiée par les techniciens

    @Column(name = "TKTSOL", columnDefinition = "TEXT")
    private String solutionProcedure; // Procédure de résolution appliquée

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "CLIID", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "USEID", nullable = false)
    private AppUser createdBy;

    @Column(name = "MISINT")
    private String assignedTo; // Nom du technicien ACE3i assigné

    @Column(name = "TKTCDT", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "TKTUDT")
    private LocalDateTime updatedAt;

    @Column(name = "TKTRDT")
    private LocalDateTime resolvedAt;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<TicketComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<TicketAttachment> attachments = new ArrayList<>();

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onPreUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
