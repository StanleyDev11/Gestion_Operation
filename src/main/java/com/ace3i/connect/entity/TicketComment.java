package com.ace3i.connect.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "CNTCM")
@Data
@NoArgsConstructor
public class TicketComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TCMID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TKTID", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "USEID", nullable = false)
    private AppUser author;

    @NotBlank(message = "Le message ne peut pas être vide")
    @Column(name = "TCMCNT", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "TCMINT", nullable = false)
    private Boolean internalOnly = false; // Si true, visible uniquement par techniciens/admin ACE3i

    @Column(name = "TCMCDT", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "TCMUDT")
    private LocalDateTime updatedAt;

    @Column(name = "TCMEDT")
    private Boolean edited = false;

    public boolean isEdited() {
        return Boolean.TRUE.equals(edited);
    }

    public boolean isInternalOnly() {
        return Boolean.TRUE.equals(internalOnly);
    }

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("createdAt ASC")
    private List<TicketAttachment> attachments = new ArrayList<>();

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
