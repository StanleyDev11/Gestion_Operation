package com.ace3i.connect.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CNUSE")
@Data
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USEID")
    private Long id;

    @NotBlank(message = "L'identifiant est obligatoire")
    @Size(min = 3, max = 50, message = "L'identifiant doit contenir entre 3 et 50 caractères")
    @Column(name = "USENAM", unique = true, nullable = false)
    private String username;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Column(name = "USEPWD", nullable = false)
    private String password;

    @NotBlank(message = "Le nom complet est obligatoire")
    @Column(name = "USEFUL")
    private String fullName;
    
    @NotBlank(message = "Le rôle est obligatoire")
    @Column(name = "USEROL")
    private String role; // ROLE_ADMIN ou ROLE_TECHNICIAN

    @Column(name = "USEACT")
    private boolean active = true;
}
