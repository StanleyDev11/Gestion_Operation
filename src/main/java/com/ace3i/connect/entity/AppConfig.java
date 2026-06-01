package com.ace3i.connect.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CNCFG")
@Data
@NoArgsConstructor
public class AppConfig {

    @Id
    @Column(name = "CFGID")
    private Long id = 1L; // Toujours l'ID 1 pour une config unique

    @Column(name = "CFGNAM")
    private String companyName = "ACE3i AFRICA";

    @Column(name = "CFGADD")
    private String address = "Rue KATIOLA, Camp R.I.T. - BP 1068 Lomé";

    @Column(name = "CFGPHO")
    private String phone = "+228 22 21 39 31";

    @Column(name = "CFGMAL")
    private String email = "infos@ace3i.com";

    @Column(name = "CFGWEB")
    private String website = "www.ace3i.com";

    @Column(name = "CFGLOL", columnDefinition = "LONGTEXT")
    private String logoLeft; // Base64

    @Column(name = "CFGLOR", columnDefinition = "LONGTEXT")
    private String logoRight; // Base64

    @Column(name = "CFGWAT", columnDefinition = "LONGTEXT")
    private String watermarkImage; // Image du filigrane (Base64)

    // Configuration Email Notifications
    @Column(name = "CFGNTF")
    private String notificationEmail; // Email qui recevra les alertes

    @Column(name = "CFGSMT_H")
    private String smtpHost;

    @Column(name = "CFGSMT_P")
    private Integer smtpPort = 587;

    @Column(name = "CFGSMT_U")
    private String smtpUsername;

    @Column(name = "CFGSMT_W")
    private String smtpPassword;

    @Column(name = "CFGSMT_A")
    private Boolean smtpAuth = true;

    @Column(name = "CFGSMT_S")
    private Boolean smtpStartTls = true;
}
