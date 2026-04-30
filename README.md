# 🚀 LIMTIC Backend — Laboratoire de Recherche

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.13-brightgreen.svg)
![Java](https://img.shields.io/badge/Java-17-orange.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-latest-blue.svg)
![Security](https://img.shields.io/badge/Security-JWT-red.svg)

Bienvenue sur le backend du projet **LIMTIC**. Cette application fournit une API robuste pour la gestion des activités de recherche du laboratoire, incluant les chercheurs, les publications, les événements et les axes de recherche.

## 🌟 Fonctionnalités Clés

-   **Gestion des Membres** : Chercheurs, Doctorants et Mastériens.
-   **Activités Scientifiques** : Gestion des publications et des axes de recherche.
-   **Événementiel** : Organisation et suivi des événements du laboratoire (avec support photo).
-   **Sécurité Avancée** :
    -   Authentification par **JWT (JSON Web Tokens)**.
    -   Contrôle d'accès basé sur les rôles (**RBAC** : ADMIN, SUPER_ADMIN).
    -   Protection contre les attaques par force brute (**Rate Limiting** avec Bucket4j).
    -   Vérification par **hCaptcha**.
-   **Audit & Monitoring** : Journalisation complète des actions administratives via `AuditLog`.
-   **Emailing** : Support SMTP pour les notifications et la récupération de mot de passe.
-   **Outils de Données** : Import et Export au format CSV.
-   **Documentation API** : Intégration Swagger/OpenAPI.

## 🛠️ Stack Technique

-   **Framework Principal** : Spring Boot 3.5.x
-   **Langage** : Java 17
-   **Accès aux Données** : Spring Data JPA (Hibernate)
-   **Base de Données** : PostgreSQL
-   **Sécurité** : Spring Security + JJWT
-   **Documentation** : Springdoc OpenAPI (Swagger UI)
-   **Utilitaires** : Lombok, Dotenv, Bucket4j, Hibernate Validator

## 📋 Prérequis

-   **Java 17** ou supérieur.
-   **Maven 3.6+** (ou utiliser le `mvnw` fourni).
-   **PostgreSQL** installé et configuré.
-   Un certificat SSL/Keystore (`keystore.p12`) dans `src/main/resources`.

## ⚙️ Configuration

1.  **Variables d'environnement** :
    Créez un fichier `.env` à la racine du projet en vous basant sur l'exemple ci-dessous :

    ```env
    DB_URL=jdbc:postgresql://localhost:5432/limtic_db
    DB_USERNAME=votre_utilisateur
    DB_PASSWORD=votre_mot_de_passe
    SMTP_USERNAME=votre_email@etudiant-isi.utm.tn
    SMTP_PASSWORD=votre_app_password_google
    SSL_KEYSTORE_PASSWORD=votre_password_keystore
    HCAPTCHA_SECRET=votre_secret_hcaptcha
    ```

2.  **Base de données** :
    Assurez-vous que la base de données spécifiée dans `DB_URL` existe.

## 🚀 Lancement

### Windows (PowerShell)
Utilisez le script fourni pour charger les variables d'environnement et lancer l'application :
```powershell
./start.ps1
```

### Via Maven (Standard)
```bash
./mvnw spring-boot:run
```

L'application sera disponible sur **`https://localhost:8443`**.

## 📖 Documentation API

Une fois l'application lancée, vous pouvez accéder à l'interface Swagger pour explorer et tester les points d'entrée :

🔗 **[https://localhost:8443/swagger-ui](https://localhost:8443/swagger-ui)**

> [!NOTE]
> L'accès au Swagger est protégé et nécessite une authentification avec un compte possédant le rôle `ADMIN`.

## 📂 Structure du Projet

```text
src/main/java/tn/limtic/limtic_backend/
├── config/         # Configuration Sécurité, JWT, CORS, MVC
├── controller/     # Points d'entrée de l'API (REST Controllers)
├── filter/         # Filtres (Rate Limiting, etc.)
├── model/          # Entités JPA (Domaine)
├── repository/     # Interfaces d'accès aux données (Spring Data)
└── service/        # Logique métier et intégrations (SMTP, Audit, etc.)
```

---
© 2024 Laboratoire LIMTIC. Tous droits réservés.
