package com.ace3i.connect.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "CNAPPVERSION")
@Data
@NoArgsConstructor
public class ApplicationVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VERID")
    private Long id;

    @NotBlank(message = "Le numéro de version est obligatoire")
    @Column(name = "VERNUM", unique = true)
    private String versionNumber;

    @Column(name = "VERPRJ")
    private String projectName;

    @Column(name = "VERBRA")
    private String branchName;

    @Column(name = "VERDATE")
    private LocalDate releaseDate;

    @Column(name = "VERMASTER")
    private Boolean isLatestMaster = false; // Default to false

    @Column(name = "VERDESC")
    private String description;
}
