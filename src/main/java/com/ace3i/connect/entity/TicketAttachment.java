package com.ace3i.connect.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "CNTAT")
@Data
@NoArgsConstructor
public class TicketAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TATID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TKTID", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TCMID")
    private TicketComment comment; // Optionnel : rattaché à un commentaire spécifique

    @Column(name = "TATNAM", nullable = false)
    private String fileName; // Nom original du fichier

    @Column(name = "TATSNA", nullable = false)
    private String storedFileName; // Nom stocké sur le disque (ex: UUID.png)

    @Column(name = "TATTYP")
    private String fileType; // Type MIME (image/png, text/plain, etc.)

    @Column(name = "TATSZ")
    private Long fileSize; // Taille en octets

    @Column(name = "TATCDT", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
