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
- Docker & Docker Compose (pour MySQL 8)

## Démarrage rapide

```bash
# 1. Démarrer MySQL 8 (et Adminer sur http://localhost:8081)
docker compose up -d

# 2. Lancer l'application (http://localhost:8080)
mvn spring-boot:run
```

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
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `3306` / `prdv` | Base MySQL |
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

## Structure

```
src/main/java/com/prdv/rdv/
├── common/                      Erreurs et Web communes
└── iam/
    ├── domain/                 Cœur métier pur (JPA/ Spring)
    ├── application/            Cas d'usage (ports entrants), ports sortants
    └── adapter/
        ├── in/web, in/security REST, JWT, OAuth2, rate limit
        └── out/…               JPA/MySQL, JWT, OTP, KYC, fraude, stockage…
```
