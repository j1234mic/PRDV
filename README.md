# PRDV — Plateforme de prises de rendez-vous médicaux

Projet Spring Boot 3 (Java 17) organisé en **architecture hexagonale**
(ports & adaptateurs), suivant les principes **SOLID** et des design patterns
classiques (Adapter, Strategy, Observer, Factory, Specification, Value Object…).

## Module 1 — Gestion des utilisateurs & authentification

- Inscription multi-profils : **patients, praticiens, secrétaires médicales,
  établissements, administrateurs**
- Vérification **OTP** email/SMS, validation KYC (CNI/passeport), profil mineur rattaché
- Praticiens : vérification **RPPS/ADELI** et diplômes (API Ordre des médecins),
  RIB tokenisé, assurance RC pro, contrat électronique, **multi-établissements**,
  gestion des **remplaçants**
- Authentification : mot de passe + **JWT/refresh tournants**, **MFA TOTP**
  (Google Authenticator), OTP de secours, **OAuth2/OIDC** (Google, SSO entreprise)
- Sécurité : verrouillage après échecs, détection de fraude (nouveau pays,
  « impossible travel »), rate limiting, chiffrement **AES-GCM**, tokenisation
  des données sensibles, audit trail complet (IP, terminal, géoloc)
- **RBAC** granulaire : rôles prédéfinis/personnalisés, permissions par module,
  **délégation temporaire** de droits, révocation instantanée
- Import de données depuis une autre plateforme (anti-corruption, CSV)
- Données analytics **anonymisées** (RGPD) et droit à l'oubli

> Le détail de l'architecture, des patterns et la table de couverture des
> exigences figurent dans [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Prérequis

- JDK **17+**
- Maven **3.9+**
- Docker & Docker Compose (optionnel, pour MySQL 8) — sinon H2 embarquée est utilisée par défaut

## Démarrage rapide

### Option A — Sans Docker (H2 embarquée, par défaut) — le plus simple

Idéal pour démarrer rapidement ou quand Docker n'est pas disponible.
Aucune base à installer : une base fichier `./data/prdv.mv.db` est créée automatiquement.

```bash
# Lancer directement (http://localhost:8080)
mvn spring-boot:run
```

- H2 Console (debug) : http://localhost:8080/h2-console
  - JDBC URL : `jdbc:h2:file:./data/prdv`
  - User : `sa`, pas de mot de passe
- Swagger UI : http://localhost:8080/swagger-ui.html

> ⚠️ H2 2.x **refuse** `AUTO_SERVER=TRUE` combiné à `DB_CLOSE_ON_EXIT=FALSE`
> (`Feature not supported: "AUTO_SERVER=TRUE && DB_CLOSE_ON_EXIT=FALSE" [50100]`) :
> l'URL par défaut n'utilise donc pas `AUTO_SERVER`. La console H2 intégrée n'en a pas
> besoin (elle tourne dans le même JVM et se rattache à la base déjà ouverte).
>
> Pour ouvrir `./data/prdv.mv.db` depuis un **outil externe** (DBeaver, IntelliJ…)
> pendant que l'application tourne, passez en « mixed mode » **sans**
> `DB_CLOSE_ON_EXIT=FALSE` :
>
> ```bash
> SPRING_DATASOURCE_URL='jdbc:h2:file:./data/prdv;MODE=MySQL;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE' mvn spring-boot:run
> ```

### Option B — Avec MySQL 8 (Docker) — proche prod

Recommandé pour se rapprocher de la production.

```bash
# 1. Démarrer MySQL 8 (et Adminer sur http://localhost:8081)
docker compose up -d
# ou : DB_PORT=13306 docker compose up -d  (si le port 3306 est occupé)

# 2. Lancer l'application en profil mysql (http://localhost:8080)
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
# Si le port MySQL a été changé :
# DB_PORT=13306 mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

> Grâce à `spring-boot-docker-compose`, si Docker est disponible, le conteneur
> `prdv-mysql` démarre automatiquement même sans `docker compose up -d` quand le
> profil `mysql` est actif.

### Option C — H2 en mémoire (dev rapide, sans fichier)

Si vous préférez une base éphémère en mémoire :

```bash
# Profil h2 : base H2 en mémoire, pas besoin de MySQL
mvn spring-boot:run -Dspring-boot.run.profiles=h2

# Ou profil dev (H2 + logs SQL + console H2 sur /h2-console)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Console H2 : http://localhost:8080/h2-console  
JDBC URL mémoire : `jdbc:h2:mem:prdv` / user `sa` / pas de mot de passe.

> Les profils `h2`, `dev`, `mysql` et `social` sont combinables :
> `-Dspring-boot.run.profiles=h2,social` ou `mysql,social`

### Dépannage — `Feature not supported: "AUTO_SERVER=TRUE && DB_CLOSE_ON_EXIT=FALSE"`

Erreur H2 `[50100-224]` (SQLState `HYC00`) au démarrage :

```
ERROR o.h.engine.jdbc.spi.SqlExceptionHelper : Fonctionnalité non supportée: "AUTO_SERVER=TRUE && DB_CLOSE_ON_EXIT=FALSE"
Caused by: org.h2.jdbc.JdbcSQLFeatureNotSupportedException
...
Failed to initialize JPA EntityManagerFactory ... Unable to build Hibernate SessionFactory
```

H2 2.x n'autorise pas le mode « mixed/auto-server » quand `DB_CLOSE_ON_EXIT=FALSE`
est demandé. Choisissez l'un des deux :

```bash
# (défaut du dépôt) base fichier, un seul processus
# jdbc:h2:file:./data/prdv;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
mvn spring-boot:run

# ou mixed mode (accès depuis DBeaver/IntelliJ pendant que l'app tourne), SANS DB_CLOSE_ON_EXIT
SPRING_DATASOURCE_URL='jdbc:h2:file:./data/prdv;MODE=MySQL;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE' mvn spring-boot:run

# ou base en mémoire (aucun fichier)
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

Vérifiez aussi qu'aucune surcharge locale (`SPRING_DATASOURCE_URL`,
`-Dspring-boot.run.arguments=--spring.datasource.url=…`) ne réintroduit les deux
paramètres à la fois. Détails : [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md).

### Dépannage — `Communications link failure` / `Connexion refusée`

Si vous voyez :

```
SQL Error: 0, SQLState: 08S01
Communications link failure
The last packet sent successfully to the server was 0 milliseconds ago.
Caused by: java.net.ConnectException: Connexion refusée
...
Unable to open JDBC Connection for DDL execution
```

C'est que MySQL n'est pas démarré ou inaccessible.

Solutions :

1. **Utiliser H2 par défaut** (plus simple, plus de MySQL requis) :
   ```bash
   mvn spring-boot:run
   # ou explicitement :
   mvn spring-boot:run -Dspring-boot.run.profiles=h2
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```
2. **Démarrer MySQL via Docker** :
   ```bash
   docker compose up -d
   docker compose ps   # doit afficher prdv-mysql healthy
   mvn spring-boot:run -Dspring-boot.run.profiles=mysql
   ```
3. **Port 3306 déjà occupé** (MySQL natif installé) :
   ```bash
   lsof -i :3306
   DB_PORT=13306 docker compose up -d
   DB_PORT=13306 mvn spring-boot:run -Dspring-boot.run.profiles=mysql
   ```
4. **Vérifier les variables** `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD` si vous utilisez un MySQL externe.

Le détail complet est dans [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md).
Un `DatabaseConnectionFailureAnalyzer` affiche désormais un message d'aide directement
au démarrage en cas d'échec de connexion (MySQL injoignable, URL H2 invalide, fichier
H2 verrouillé).

Swagger UI : <http://localhost:8080/swagger-ui.html>

Au démarrage, le catalogue de permissions, les rôles systèmes et un compte
**super-administrateur** sont créés :

| Email                         | Mot de passe par défaut |
|-------------------------------|-------------------------|
| `superadmin@prdv.app`         | `Admin#2026!`           |

⚠️ Changez ces valeurs en production via les variables d'environnement
`ADMIN_EMAIL` / `ADMIN_PASSWORD`.

## Variables d'environnement

| Variable | Défaut | Rôle |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `3306` / `prdv` | Base MySQL (profil `mysql`) |
| `DB_USER` / `DB_PASSWORD` | `root` / `root` | Identifiants MySQL |
| `JWT_SECRET` | secret de dev | Clé HMAC des JWT (≥ 32 octets) |
| `CIPHER_SECRET` | secret de dev | Clé mère du chiffrement AES-GCM |
| `STORAGE_PATH` | `./storage/kyc` | Stockage local des pièces KYC |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | ci-dessus | Super-administrateur initial |
| `FRONTEND_OAUTH_URL` | `http://localhost:3000/auth/callback` | Redirection post-login social |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | — | Activer l'envoi d'emails réels (sinon les OTP sont dans les logs) |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | — | Login Google (profil `social`) |

Activer le login social / SSO :

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=social
# ou avec H2 : mvn spring-boot:run -Dspring-boot.run.profiles=h2,social
# ou avec MySQL : mvn spring-boot:run -Dspring-boot.run.profiles=mysql,social
```

## Parcours d'API (abrégé)

```
POST /api/v1/auth/register/patient           Inscription patient → OTP
POST /api/v1/auth/otp/verify                 Confirmation OTP
POST /api/v1/auth/login                      Connexion (jetons ou défi MFA/OTP)
POST /api/v1/auth/login/totp                 Validation TOTP
POST /api/v1/auth/refresh                    Rotation du refresh token
GET  /api/v1/me                              Profil connecté
POST /api/v1/practitioners/register          Inscription praticien
POST /api/v1/practitioners/me/contract       Contrat d'adhésion
POST /api/v1/practitioners/me/memberships    Rattachement à un cabinet
POST /api/v1/establishments/register         Inscription établissement
POST /api/v1/secretaries (via praticien)      Création secrétaire
POST /api/v1/admin/applications/{id}/review  Validation manuelle (admin)
GET  /api/v1/admin/audit                     Journal d'audit
```

Un fichier d'exemples complet : [docs/api-m1.http](docs/api-m1.http).
Les codes OTP en développement s'affichent dans les logs :

```
[NOTIFICATION EMAIL] Code OTP pour patient@… : 384921
```

## Tests

```bash
mvn test
```

Les tests utilisent H2 en mémoire (voir `src/test/resources/application.yml`).

## Structure

```
src/main/java/com/prdv/rdv/
├── common/                      Erreurs et Web communes
└── iam/
    ├── domain/                 Cœur métier pur (JPA/ Spring)
    ├── application/            Cas d'usage (ports entrants), ports sortants
    └── adapter/
        ├── in/web, in/security REST, JWT, OAuth2, rate limit
        └── out/…               JPA/MySQL/H2, JWT, OTP, KYC, fraude, stockage…
```
