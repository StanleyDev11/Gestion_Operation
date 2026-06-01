package com.ace3i.connect.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.Duration;

@Entity
@Table(name = "CNMIS")
@Data
@NoArgsConstructor
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MISID")
    private Long id;

    @NotBlank(message = "Le nom du projet est obligatoire")
    @Column(name = "MISPRJ")
    private String projectName;

    @NotBlank(message = "Le numéro d'ordre est obligatoire")
    @Column(name = "MISORD")
    private String orderNumber;

    @NotBlank(message = "Le nom du client est obligatoire")
    @Column(name = "MISCLI")
    private String clientName;

    @NotBlank(message = "L'intervenant ACE3i est obligatoire")
    @Column(name = "MISINT")
    private String intervener;

    @Column(name = "MISICL")
    private String clientIntervener; // Nouveau champ pour le contact banque

    @NotNull(message = "La date de début est obligatoire")
    @Column(name = "MISDEB")
    private LocalDateTime startDate;

    @NotNull(message = "La date de fin est obligatoire")
    @Column(name = "MISFIN")
    private LocalDateTime endDate;

    @Column(name = "MISDAY")
    private Double nbDaysToInvoice;

    @Column(name = "MISTYP")
    private String interventionType;

    @Column(name = "MISSTA")
    private String status = "Planifiée"; // Planifiée, En cours, Terminée

    @Column(name = "MISOPR")
    private String operationType; // En ligne, En présentiel

    @Column(name = "MISOTN")
    private String taskNumber; // Numéro de tâche OpenProject

    @Column(name = "MISOTL", length = 1000)
    private String taskLink;   // Lien vers la tâche OpenProject

    // Profils
    @Column(name = "MISPCH")
    private boolean profileChefProjet;

    @Column(name = "MISPCO")
    private boolean profileConsultant;

    // Types d'intervention détaillés
    @Column(name = "MISTAN")
    private boolean typeAnalyse;

    @Column(name = "MISTFO")
    private boolean typeFormation;

    @Column(name = "MISTMA")
    private boolean typeMaintenance;

    @Column(name = "MISTPR")
    private boolean typeProgrammation;

    @Column(name = "MISTAS")
    private boolean typeAssistance;

    @Column(name = "MISTIN")
    private boolean typeInstallation;

    @Column(name = "MISTME")
    private boolean typeMiseEnProduction;

    @Column(name = "MISTSU")
    private boolean typeSuiviProjet;

    @Column(name = "MISTCO")
    private Boolean typeCorrectif = false;

    @Column(name = "MISTIC")
    private Boolean typeIncident = false;

    @Column(name = "MISTSP")
    private Boolean typeSupport = false;

    @Column(name = "MISTEV")
    private Boolean typeEvolution = false;

    @Column(name = "MISSER")
    private String serviceConcerne;

    @Column(name = "MISPDS", columnDefinition = "TEXT")
    private String problemDescription;

    @Column(name = "MISCAU", columnDefinition = "TEXT")
    private String rootCause;

    @Column(name = "MISSOL", columnDefinition = "TEXT")
    private String solutionProcedure;

    @Column(name = "MISDES", columnDefinition = "TEXT")
    @Size(max = 5000, message = "La description ne peut pas dépasser 5000 caractères")
    private String description; // "Travaux réalisés"

    @Column(name = "MISOCL", columnDefinition = "TEXT")
    @Size(max = 2000, message = "Les observations client ne peuvent pas dépasser 2000 caractères")
    private String clientObservations;

    @Column(name = "MISOIN", columnDefinition = "TEXT")
    @Size(max = 2000, message = "Les observations intervenant ne peuvent pas dépasser 2000 caractères")
    private String intervenerObservations;

    // Durée calculée : "X jours, Y heures" ou similaire
    public String getCalculatedDuration() {
        if (startDate == null || endDate == null) return "N/A";
        
        Duration duration = Duration.between(startDate, endDate);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("j ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("min");
        
        return sb.toString().trim().isEmpty() ? "0 min" : sb.toString().trim();
    }
}
