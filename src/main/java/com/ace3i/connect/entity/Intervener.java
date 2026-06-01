package com.ace3i.connect.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CNINT")
@Data
@NoArgsConstructor
public class Intervener {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "INTID")
    private Long id;

    @NotBlank(message = "Le nom de l'intervenant est obligatoire")
    @Column(name = "INTNAM")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INTCLI", nullable = false)
    @com.fasterxml.jackson.annotation.JsonBackReference
    private Client client;
}
