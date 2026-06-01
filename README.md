# ACE3i Connect - Système de Suivi des Interventions

ACE3i Connect est une application web de gestion et de suivi des missions d'intervention technique. Elle permet de centraliser les rapports de prestation, de suivre l'état des tâches et de générer des fiches d'intervention professionnelles au format PDF.

## 🚀 Fonctionnalités Principales

- **Suivi des Missions** : Saisie détaillée des interventions (Projet, Client, Intervenants, Durée).
- **Analyse du Problème** : Section dédiée pour le service concerné, la description du problème et l'analyse de la cause racine.
- **Types d'Intervention** : Gestion de 12 types d'intervention (Correctif, Incident, Support, Évolution, Formation, Installation, etc.).
- **Génération PDF** : Export automatique des fiches de prestation et rapports périodiques.
- **Suivi Fonctionnalités** : Mapping des fonctionnalités déployées par client et par version.
- **Administration** : Gestion du personnel, journal d'audit complet et monitoring de la santé système.

## 🛠️ Stack Technique

- **Backend** : Java 17+, Spring Boot 3.x
- **Persistance** : Spring Data JPA, Hibernate
- **Base de données** : MySQL 8.x
- **Frontend** : Thymeleaf, Bootstrap 5, FontAwesome 6
- **PDF** : iText 7
- **Sécurité** : Spring Security (RBAC - Admin/User)

## 📋 Prérequis

Avant de commencer, assurez-vous d'avoir installé :
- [Java JDK 17+](https://www.oracle.com/java/technologies/downloads/)
- [Maven 3.8+](https://maven.apache.org/)
- [MySQL Server 8.0+](https://dev.mysql.com/downloads/mysql/)

## ⚙️ Configuration

1. **Base de données** :
   - Créez une base de données MySQL nommée `db_missions`.
   ```sql
   CREATE DATABASE db_missions CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. **Configuration de l'application** :
   - Modifiez le fichier `src/main/resources/application.properties` pour configurer vos accès :
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/db_missions?createDatabaseIfNotExist=true&serverTimezone=UTC
   spring.datasource.username=votre_utilisateur
   spring.datasource.password=votre_mot_de_passe
   
   # Mode de mise à jour de la base (update pour le développement)
   spring.jpa.hibernate.ddl-auto=update
   ```

## 🏃 Démarrage (Développement)

Pour lancer l'application en mode développement :

```bash
mvn spring-boot:run
```
L'application sera accessible sur : `http://localhost:8087`

**Identifiants par défaut** (si initialisés) :
- Admin : `admin` / `admin123`
- User : `user` / `user123`

## 📦 Build et Packaging

Pour générer le fichier exécutable JAR :

```bash
mvn clean package -DskipTests
```
Le fichier JAR sera généré dans le dossier `target/ace3i-connect-0.0.1-SNAPSHOT.jar`.

## 🚢 Déploiement

### Déploiement Standard (Serveur Linux/Windows)

1. Transférez le fichier `.jar` sur votre serveur.
2. Lancez l'application en tant que service ou via la ligne de commande :
   ```bash
   java -jar ace3i-connect-0.0.1-SNAPSHOT.jar
   ```

### Recommandations de Production
- Changez `spring.jpa.hibernate.ddl-auto` à `validate` ou `none`.
- Utilisez un reverse proxy comme **Nginx** pour gérer le SSL (HTTPS).
- Configurez des variables d'environnement pour les secrets (mots de passe DB).

## 📄 Licence

Ce projet est la propriété exclusive de **ACE3i**. Tous droits réservés.
