# DOSSIER D'ARCHITECTURE TECHNIQUE & FONCTIONNELLE (DAT / DAF)
## Application : ACE3i Connect — Plateforme de Pilotage, Support & Intégration Bancaire
**Société :** ACE3i AFRICA  
**Version de l'application :** 1.2.0-SNAPSHOT  
**Date du document :** Novembre 2026  
**Destinataires :** Analystes Fonctionnels, Chefs de Projets, Développeurs & Administrateurs Système  

---

## SOMMAIRE
1. [Contexte Général & Objectifs Métier](#1-contexte-général--objectifs-métier)
2. [Périmètre Bancaire & Modulaire](#2-périmètre-bancaire--modulaire)
3. [Architecture Technique & Stack Logicielle](#3-architecture-technique--stack-logicielle)
4. [Dictionnaire des Données & Schéma Relationnel](#4-dictionnaire-des-données--schéma-relationnel)
5. [Spécifications Fonctionnelles des Modules](#5-spécifications-fonctionnelles-des-modules)
   - [5.1. Module Missions & Interventions Techniques](#51-module-missions--interventions-techniques)
   - [5.2. Module Support Helpdesk (Incidents & Demandes)](#52-module-support-helpdesk-incidents--demandes)
   - [5.3. Module Pilotage des Fonctionnalités & Versions](#53-module-pilotage-des-fonctionnalités--versions)
   - [5.4. Module Intégration GitLab (Synchronisation Automatique)](#54-module-intégration-gitlab-synchronisation-automatique)
   - [5.5. Module Administration, Sécurité & Audit](#55-module-administration-sécurité--audit)
6. [Matrice des Droits d'Accès & Habilitations](#6-matrice-des-droits-daccès--habilitations)
7. [Guide d'Installation, Déploiement & Exploitation (Runbook)](#7-guide-dinstallation-déploiement--exploitation-runbook)
8. [Bonnes Pratiques de Développement & Résolution d'Incidents](#8-bonnes-pratiques-de-développement--résolution-dincidents)

---

## 1. CONTEXTE GÉNÉRAL & OBJECTIFS MÉTIER

### 1.1. Contexte
**ACE3i AFRICA** est une entreprise d'ingénierie logicielle spécialisée dans l'intégration, la maintenance et le déploiement de solutions informatiques financières et bancaires en Afrique de l'Ouest. Elle intervient comme partenaire stratégique pour de grandes institutions bancaires.

### 1.2. Problématique résolue par ACE3i Connect
Auparavant, le suivi des interventions, la déclaration des incidents par les banques et le suivi des versions logicielles étaient dispersés (échanges d'e-mails, fichiers Excel manuels, appels téléphoniques).  
**ACE3i Connect** centralise l'ensemble des processus opérationnels en une interface unique :
* **Un guichet unique de support** pour les banques partenaires (déclaration et suivi d'incidents).
* **Une traçabilité juridique et contractuelle des interventions** via la génération de rapports de mission PDF signables.
* **Un observatoire en temps réel des versions déployées** grâce à une synchronisation directe avec le dépôt GitLab interne de l'entreprise.

---

## 2. PÉRIMÈTRE BANCAIRE & MODULAIRE

### 2.1. Banques Partenaires Clés
L'application cloisonne les accès et centralise les données pour les 5 banques partenaires suivantes :
1. **SUNU**
2. **IB-BANK**
3. **UTB (Union Togolaise de Banque)**
4. **CORIS (Coris Bank International)**
5. **ORABANK**

### 2.2. Domaines & Modules Applicatifs Pris en Charge
Le support et les interventions techniques sont structurés autour de 5 grandes catégories officielles :
* **AMPLITUDE :** Progiciel Core Banking (comptabilité générale, guichet, clôtures journalières/mensuelles, calculs d'intérêts, crédits, engagements).
* **EPAIEMENT :** Passerelles de paiement en ligne, protocoles marchands, monétique e-commerce, télépaiement.
* **MBANKING :** Applications mobiles bancaires pour clients finaux (Android / iOS), authentification OTP, consultations de comptes, virements mobiles.
* **SEGUCE :** Guichet Unique pour le Commerce Extérieur (intégration des flux portuaires, quittances douanières, téléprocédures maritimes).
* **OTR :** Office Togolais des Recettes (passerelle de télécollecte des impôts, taxes et droits fiscaux pour le compte de l'État).

---

## 3. ARCHITECTURE TECHNIQUE & STACK LOGICIELLE

### 3.1. Composants Techniques
* **Langage :** Java 21 LTS
* **Framework Principal :** Spring Boot 3.3.x
* **Moteur de Templates :** Thymeleaf 3.1 (avec dialectes Spring Security et Layout)
* **Couche de Persistance :** Spring Data JPA / Hibernate ORM
* **Base de Données :** MySQL 8.0+ (InnoDB, UTF-8 MB4)
* **Sécurité & Authentification :** Spring Security 6 (BCrypt Password Hashing, Session Management, CSRF)
* **Génération de Documents PDF :** iText 7 / OpenPDF avec moteur de templating HTML vers PDF
* **Communication API :** `java.net.http.HttpClient` natif JDK 21 (sans dépendance externe lourde) avec support des certificats SSL auto-signés
* **Gestionnaire de dépendances :** Apache Maven 3.9+
* **Interface Utilisateur :** Bootstrap 5, FontAwesome 6, Chart.js, SweetAlert2

### 3.2. Architecture en Couches (MVC DDD-Light)
```
com.ace3i.connect
├── config         # Configuration Spring Security, Initialiseurs de démarrage
├── controller     # Contrôleurs web Thymeleaf & Endpoints REST AJAX
├── entity         # Entités JPA mappées sur la base MySQL (Convention CNxxx)
├── repository     # Interfaces Spring Data JPA (Requêtes natives et dérivés)
├── service        # Interfaces des services métier
│   └── impl       # Implémentations concrètes (TicketService, GitLabService, MissionService...)
└── scheduler      # Tâches planifiées (Cron d'alertes de retard de mission)
```

---

## 4. DICTIONNAIRE DES DONNÉES & SCHÉMA RELATIONNEL

Toutes les tables du système utilisent le préfixe unifié **`CN`** (*Connect*) et des noms de colonnes courts et normalisés :

| Table | Description Métier | Colonnes Clés |
|---|---|---|
| **`CNCLI`** | Banques & Clients Partenaires | `CLIID` (PK), `CLINAM` (Nom), `CLIBRA` (Branche Git associée) |
| **`CNUSE`** | Utilisateurs (Admin, Techniciens, Clients) | `USEID` (PK), `USENAM` (Login), `USEPWD` (Hash), `USEROL` (Rôle), `CLIID` (FK Banque) |
| **`CNMIS`** | Missions & Interventions Techniques | `MISID` (PK), `MISORD` (Ordre), `MISCLI` (Banque), `MISINT` (Expert ACE3i), `MISDAY` (Jours), `MISSTA` (Statut) |
| **`CNTKT`** | Tickets Helpdesk (Incidents / Demandes) | `TKTID` (PK), `TKTNUM` (Numéro), `TKTTYP` (Type), `TKTAPP` (Module), `TKTURG` (Urgence), `TKTSTA` (Statut), `CLIID` (FK) |
| **`CNTCM`** | Messages & Conversations de Tickets | `TCMID` (PK), `TKTID` (FK Ticket), `USEID` (Auteur), `TCMCNT` (Texte), `TCMINT` (Note Interne 0/1), `TCMEDT` (Édité 0/1) |
| **`CNTAT`** | Pièces Jointes des Tickets & Messages | `TATID` (PK), `TKTID` (FK Ticket), `TCMID` (FK Message), `TATNAM` (Nom), `TATSNA` (Fichier Stocké) |
| **`CNTCA`** | Catégories & Modules de Tickets | `TCAID` (PK), `TCANAM` (Nom), `TCAORD` (Ordre), `TCAACT` (Actif 0/1) |
| **`CNCFG`** | Configuration Générale & GitLab | `CFGID` (PK 1), `CFGGL_U` (URL GitLab), `CFGGL_T` (Token), `CFGGL_P` (IDs Projets), `CFGGL_B` (Branche master) |
| **`CNAPPVERSION`**| Versions & Déploiements Applicatifs | `VERID` (PK), `VERNUM` (Numéro version/tag), `VERPRJ` (Projet), `VERBRA` (Branche Git), `VERDATE` (Date) |
| **`CNAUD`** | Journal d'Audit et de Traçabilité | `AUDID` (PK), `AUDDAT` (Date), `USENAM` (Auteur), `AUDACT` (Action), `AUDDET` (Détail) |

---

## 5. SPÉCIFICATIONS FONCTIONNELLES DES MODULES

### 5.1. Module Missions & Interventions Techniques
* **Objet :** Enregistrement des travaux d'assistance, formation, programmation, maintenance ou déploiement réalisés par les experts ACE3i pour les banques.
* **Fonctionnalités :**
  - Numérotation d'ordre automatique (`ORD-AAAA-XXXX`).
  - Précision du type d'opération : *En présentiel* ou *En ligne / Distanciel*.
  - Saisie des profils intervenants : Chef de projet, Consultant, Technicien.
  - Calcul automatique de la durée réelle (`Xj Yh Zmin`) et saisie du nombre de jours à facturer (`MISDAY`).
  - Intégration OpenProject : rattachement optionnel d'un numéro de lot (`MISOTN`) et lien direct vers le ticket OpenProject (`MISOTL`).
  - **Génération du Rapport PDF :** Fiche de mission officielle avec logos d'entreprise, filigrane, cadre de description des travaux et zones de signatures conjointes (Banque / ACE3i).
  - Tâche planifiée (`MissionAlertScheduler`) : alerte par e-mail en cas de mission en retard ou non clôturée.

### 5.2. Module Support Helpdesk (Incidents & Demandes)
* **Objet :** Gestion du cycle de vie des demandes d'assistance et incidents techniques émis par les banques partenaires.
* **Fonctionnalités :**
  - **Types de tickets :**
    - `INCIDENT` : Dysfonctionnement d'un service existant.
    - `DEMANDE` : Évolution, configuration, création de profil ou extraction de données.
  - **Niveaux d'urgence :** `FAIBLE`, `NORMALE`, `URGENTE`, `BLOQUANTE`.
  - **Statuts :** `NOUVEAU` ➔ `EN_COURS` ➔ `EN_ATTENTE` ➔ `RESOLU` ➔ `CLOS`.
  - **Cloisonnement strict :** Une banque ne voit **que** ses propres tickets. Les techniciens et administrateurs ACE3i voient l'ensemble des tickets de toutes les banques.
  - **Fil de conversation interactif :**
    - Échanges de messages en direct.
    - Dépôt de fichiers joints par le client et par le technicien.
    - **Notes Internes Confidentielles (`internalOnly = true`) :** Visibles uniquement par l'équipe interne ACE3i, strictement invisibles pour les clients.
    - **Modification d'un message :** L'auteur ou l'administrateur peut modifier son message avec affichage de la mention *"modifié"* et traçabilité en base.

### 5.3. Module Pilotage des Fonctionnalités & Versions
* **Objet :** Fournir une vision analytique à la direction sur l'état de conformité des banques par rapport aux versions logicielles cibles.
* **Fonctionnalités :**
  - **KPIs Globaux :** Version Maîtresse actuelle, taux d'adoption moyen, nombre de banques à jour.
  - **Matrice de Couverture Croisée :** Tableau croisé dynamique affichant chaque fonctionnalité et son état de déploiement (Présent / Absent) par banque.
  - **Filtre Clients < 80% :** Identification instantanée des banques accusant un retard de mise à niveau.
  - **Génération de Fiche d'Impression :** Export imprimable du statut fonctionnel spécifique d'une banque.

### 5.4. Module Intégration GitLab (Synchronisation Automatique)
* **Objet :** Automatiser la découverte des versions logicielles et des commits des banques directement depuis le serveur GitLab interne (`https://git.ace3i.com/`).
* **Fonctionnalités :**
  - **Multi-Projets supporté :** Configuration possible de plusieurs IDs de dépôts (ex: `22, 25, 30`) pour scanner simultanément le Core Banking, l'E-Banking et les modules satellites.
  - **Liaison Banque ➔ Branche Git :** Dans la fiche de chaque banque, association directe du nom de sa branche (ex: `sunu`, `coris`, `utb`).
  - **Synchronisation Sécurisée :** Utilisation de l'API REST v4 de GitLab via Personal Access Token ou Project Access Token avec le scope `read_api`.
  - **Support SSL Interne :** Gestion native des certificats auto-signés du réseau local.
  - **Deux modes de synchronisation :**
    1. *Par banque* : bouton d'actualisation instantané sur la ligne de la banque.
    2. *Global* : bouton « Synchroniser avec GitLab » traitant l'ensemble des banques en une seule passe avec récapitulatif détaillé SweetAlert2.

### 5.5. Module Administration, Sécurité & Audit
* **Gestion des Utilisateurs :** Création, désactivation, attribution de rôle et association optionnelle à une banque.
* **Configuration des Documents :** Upload des logos d'entête (ACE3i et Partenaire), filigrane officiel, coordonnées de pied de page et configuration du serveur SMTP pour les alertes.
* **Gestion des Modules / Catégories :** Ajout, réorganisation, activation/désactivation dynamique des catégories sans redémarrage de l'application.
* **Audit & Traçabilité :** Journalisation de toutes les actions d'administration et des modifications sensibles dans la table `CNAUD`.
* **Export & Sauvegarde :** Module de dump SQL téléchargeable de la base de données.

---

## 6. MATRICE DES DROITS D'ACCÈS & HABILITATIONS

L'application applique le principe du moindre privilège avec 3 profils distincts :

| Fonctionnalité / URL | `ROLE_ADMIN` | `ROLE_TECHNICIAN` | `ROLE_CLIENT` |
|---|:---:|:---:|:---:|
| **Tableau de Bord Direction** (`/dashboard`) | ✅ | ❌ | ❌ |
| **Gestion des Missions** (`/missions/**`) | ✅ | ✅ | ❌ |
| **Traitement des Tickets Internes** (`/tickets/**`) | ✅ | ✅ | ❌ |
| **Saisie de Notes Internes sur Ticket** | ✅ | ✅ | ❌ |
| **Espace Client Helpdesk** (`/client/tickets/**`) | ✅ | ❌ | ✅ *(Ses tickets)* |
| **Matrice des Fonctionnalités** (`/feature-tracking/**`) | ✅ | ✅ | ❌ |
| **Paramétrage Banques & GitLab** (`/settings/**`) | ✅ | ❌ | ❌ |
| **Gestion du Personnel & Utilisateurs** (`/admin/users/**`) | ✅ | ❌ | ❌ |
| **Journaux d'Audit & Maintenance** (`/admin/logs`, `/database/**`) | ✅ | ❌ | ❌ |

---

## 7. GUIDE D'INSTALLATION, DÉPLOIEMENT & EXPLOITATION (RUNBOOK)

### 7.1. Prérequis Système
* **Serveur hôte :** Linux (Ubuntu Server 22.04 LTS / Debian 12 / RHEL 9) ou Windows Server
* **Java Development Kit :** OpenJDK 21 (ou Eclipse Temurin 21)
* **Base de données :** MySQL 8.0+ ou MariaDB 10.6+
* **Outil de build :** Apache Maven 3.9+
* **Accès Réseau :** Accès HTTPS vers `https://git.ace3i.com/` (port 443) et SMTP vers le serveur de messagerie (port 587 ou 465)

### 7.2. Configuration de l'Application (`application.properties`)
Fichier : `src/main/resources/application.properties`

```properties
# Port d'écoute du serveur web
server.port=8087

# Connexion à la base de données MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/ace3i_connect?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=votre_mot_de_passe
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Configuration JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# Limites d'upload des pièces jointes
spring.servlet.multipart.max-file-size=20MB
spring.servlet.multipart.max-request-size=25MB
```

### 7.3. Procédure de Compilation & Lancement
```bash
# 1. Cloner ou ouvrir le répertoire du projet
cd C:\Users\stani\Desktop\Outil

# 2. Compiler et packager l'application (en ignorant les tests si besoin)
mvn clean package -DskipTests

# 3. Lancer l'application via le JAR généré
java -jar target/ace3i-connect-0.0.1-SNAPSHOT.jar

# OU lancer directement via Maven en environnement de développement :
mvn spring-boot:run
```

L'application est immédiatement accessible à l'adresse : **`http://localhost:8087/`**

### 7.4. Comptes d'Accès Initiaux
Au tout premier démarrage sur une base de données vide, les comptes suivants sont créés automatiquement :
* **Administrateur :** Login `admin` | Mot de passe `admin123` *(Rôle : ADMIN)*
* **Technicien :** Login `tech` | Mot de passe `tech123` *(Rôle : TECHNICIAN)*

---

## 8. BONNES PRATIQUES DE DÉVELOPPEMENT & RÉSOLUTION D'INCIDENTS

### 8.1. Règle Thymeleaf 3.1 & Événements JavaScript
> [!IMPORTANT]
> **Thymeleaf 3.1 bloque l'injection directe de chaînes de caractères dans les attributs `th:onclick`** pour des raisons de sécurité contre les failles XSS.
* ❌ **Mauvaise pratique :** `th:onclick="|confirmDelete('${client.name}')|"`
* ✅ **Bonne pratique :** Utiliser des attributs HTML5 `data-*` et un écouteur JavaScript vanille :
  ```html
  <button class="btn-delete" th:attr="data-id=${client.id},data-name=${client.name}">Supprimer</button>
  ```

### 8.2. Gestion des Types Primitifs vs Wrappers en Base
> [!WARNING]
> Lorsque de nouvelles colonnes booléennes ou numériques sont ajoutées sur une table existante en production, les lignes préexistantes contiennent la valeur SQL `NULL`.
* Ne jamais utiliser le type primitif Java `boolean` ou `int` dans les entités pour les champs ajoutés a posteriori.
* Toujours utiliser les wrappers **`Boolean`** et **`Integer`** avec des accesseurs sécurisés (*Null-Safe*) :
  ```java
  public boolean isEdited() {
      return Boolean.TRUE.equals(edited);
  }
  ```

### 8.3. Appels AJAX & Protection CSRF Spring Security
Tous les endpoints d'API appelés en AJAX (`POST`, `PUT`, `DELETE`) sont soit :
1. Exemptés explicitement dans `SecurityConfig.java` via `.csrf(csrf -> csrf.ignoringRequestMatchers("/settings/gitlab/**"))`.
2. Soit appelés en fournissant le token CSRF dans les headers HTTP :
   ```javascript
   headers: {
       'X-CSRF-TOKEN': document.querySelector('input[name="_csrf"]').value
   }
   ```

---

## 9. CONCLUSION & FEUILLE DE ROUTE
L'application **ACE3i Connect** est dans un état stable, modulaire et hautement maintenable.  
Les axes d'évolution identifiés pour les prochaines versions sont :
1. **Webhooks GitLab :** Mise en place d'un listener pour actualiser les versions automatiquement dès qu'un développeur pousse un commit sur une branche d'une banque, sans attendre un clic manuel.
2. **Notifications Push Web / WebSocket :** Alerte sonore et visuelle en temps réel pour les techniciens lors de la déclaration d'un incident bloquant.
3. **Application Mobile Technicien :** Version allégée pour signature tactile directe sur tablette lors des interventions en présentiel.
