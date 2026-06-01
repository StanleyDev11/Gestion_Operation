package com.ace3i.connect.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CNFEATURE")
@Data
@NoArgsConstructor
public class Feature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FEAID")
    private Long id;

    @NotBlank(message = "Le nom de la fonctionnalité est obligatoire")
    @Column(name = "FEANAME", unique = true)
    private String name;

    @Column(name = "FEADESC")
    private String description;
}
