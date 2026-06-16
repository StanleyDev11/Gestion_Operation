# ACE3i Connect - Système de Suivi des Interventions & Analytics

ACE3i Connect est une solution métier avancée pour la gestion, le suivi et l'analyse des missions d'intervention technique. Conçue pour transformer les données opérationnelles en indicateurs décisionnels, elle permet de centraliser les rapports, de piloter la conformité applicative et de générer des rapports analytiques de haute qualité.

## 🚀 Fonctionnalités Clés

- **Pilotage des Missions** : Saisie structurée des interventions (Projet, Client, Intervenants, Durée réelle).
- **Diagnostic Technique** : Sections dédiées à l'analyse de cause racine (Root Cause Analysis) et à la description des solutions.
- **Reporting "Data Analyst"** : 
    - Génération de rapports périodiques HTML/PDF sur 6 pages.
    - Graphiques de tendance mensuelle, répartition de la charge par type et équilibre Valeur vs Support.
    - Top Projets par consommation de ressources.
- **Conformité & Versioning** : Mapping complet des fonctionnalités déployées par client et suivi des écarts de version.
- **Sécurité "Enterprise Ready"** : Intégration avec **HashiCorp Vault** pour la gestion centralisée des secrets.

## 🛠️ Stack Technique Moderne

- **Core** : Java 21, Spring Boot 3.4.1
- **Gestion des Secrets** : Spring Cloud Vault (Bootstrap Config)
- **Persistance** : Spring Data JPA (MySQL 8.x)
- **Reporting** : iText 7.2 (PDF), Chart.js (DataViz HTML)
- **Interface** : Thymeleaf, Bootstrap 5.3, FontAwesome 6
- **Configuration** : Format YAML (`application.yml` & `bootstrap.yml`)

## 📋 Prérequis

- **Java JDK 21**
- **Maven 3.9+**
- **MySQL 8.0+**
- **Docker** (Optionnel, pour le déploiement conteneurisé)
- **Vault Server** (Optionnel pour le dev local si `enabled: false`)

## ⚙️ Configuration & Secrets

L'application utilise désormais le format **YAML** et **Spring Cloud Vault**.

1. **Configuration du Démarrage (`src/main/resources/bootstrap.yml`)** :
   ```yaml
   spring:
     cloud:
       vault:
         uri: ${VAULT_ADDR}
         token: ${VAULT_TOKEN}
         kv:
           enabled: true
   ```

2. **Base de données (`src/main/resources/application.yml`)** :
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/db_missions?createDatabaseIfNotExist=true
       username: root
       password: ${DB_PASSWORD}
   ```

## 🚢 Déploiement

### Déploiement avec Docker (Recommandé)

1. **Construire l'image** :
   ```bash
   docker build -t ace3i-connect .
   ```

2. **Lancer le conteneur** :
   ```bash
   docker run -d \
     -p 8087:8087 \
     -e VAULT_ADDR='http://votre-serveur-vault:8200' \
     -e VAULT_TOKEN='votre-token-vault' \
     --name ace3i-app \
     ace3i-connect
   ```

### Déploiement Standard (JAR)
```bash
# Compilation
mvn clean package -DskipTests

# Lancement
java -jar target/ace3i-connect-0.0.1-SNAPSHOT.jar
```

## 📊 Analytics & Reporting
Le nouveau moteur de rendu permet de générer des synthèses d'activité sur 6 pages incluant :
1. Page de Garde institutionnelle.
2. Timeline Chronologique & Top Projets.
3. Tendance Mensuelle & Statuts.
4. Analyse stratégique de la Charge.
5. État de Conformité Applicative.
6. Historique détaillé & Visas.

## 📄 Propriété
Ce projet est la propriété exclusive de **ACE3i**. Toute reproduction ou utilisation sans autorisation est interdite.
