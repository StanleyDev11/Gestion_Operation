# Étape 1 : Construction (Build)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copier le fichier pom.xml et télécharger les dépendances (mise en cache)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier le code source et construire le JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Étape 2 : Exécution (Runtime)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Créer un utilisateur non-root pour la sécurité
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Copier le JAR depuis l'étape de build
COPY --from=build /app/target/*.jar app.jar

# Configuration des variables d'environnement par défaut
ENV VAULT_ADDR=http://vault:8200
ENV VAULT_TOKEN=root
ENV SERVER_PORT=8087

# Exposer le port de l'application
EXPOSE 8087

# Lancer l'application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
