package com.ace3i.connect.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CNTCA")
@Data
@NoArgsConstructor
public class TicketCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TCAID")
    private Long id;

    @NotBlank(message = "Le nom de la catégorie est obligatoire")
    @Column(name = "TCANAM", nullable = false, length = 100)
    private String name; // Ex: "Monétique (GAB / Cartes)"

    @Column(name = "TCAORD")
    private Integer displayOrder = 0;

    @Column(name = "TCAACT")
    private Boolean active = true;

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }

    public TicketCategory(String name, int displayOrder) {
        this.name = name;
        this.displayOrder = displayOrder;
        this.active = true;
    }
}
