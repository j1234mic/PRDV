# Architecture du Module 1 (IAM)

## 1. Architecture hexagonale

```
                   ┌─────────────────────────────────────────────┐
   Acteurs         │              ADAPTATEURS ENTRÉS             │
   (web, mobile,   │  REST controllers · Security/JWT · OAuth2   │
   SSO, admin)     └───────────────┬─────────────────────────────┘
                                   │  utilise (Input Ports = cas d'usage)
                   ┌───────────────▼─────────────────────────────┐
                   │            COUCHE APPLICATION               │
                   │  Use cases (services) · Commandes · Vues    │
                   │  Input ports  ──────────▶  orchestration    │
                   │  Output ports ◀──────────  abstractions     │
                   └───────────────┬─────────────────────────────┘
                                   │  implémente (Output Ports)
                   ┌───────────────▼─────────────────────────────┐
                   │                DOMAINE (pur)                │
                   │  User · Profils · RBAC · OTP · Sessions     │
                   │  KYC · Agrégats · Valeurs · Règles · Events │
                   └───────────────┬─────────────────────────────┘
                                   │
                   ┌───────────────▼─────────────────────────────┐
                   │             ADAPTATEURS SORTIS              │
                   │ JPA/MySQL · Nimbus JWT · BCrypt · TOTP      │
                   │ AES-GCM · SMTP/logs · API RPPS · KYC · CSV  │
                   └─────────────────────────────────────────────┘
```

Le **domaine ne dépend d'aucun framework** : pas d'annotations JPA, pas de Spring,
pas de HTTP. Les seules dépendances pointent vers l'intérieur
(Dependency Rule) : le module `com.prdv.rdv.iam.domain` n'importe rien
d'autre que lui-même (hors `java.*` et Lombok).

## 2. SOLID en pratique

| Principe | Mise en œuvre |
|---|---|
| **S** — Responsabilité unique | `AuthenticationService` ne fait qu'orchestrer ; la génération/rotation des jetons est dans `TokenIssuanceSupport`, l'OTP dans `OtpIssuer`, l'audit dans `AuditLogger` ; chaque adapter a une raison de changer. |
| **O** — Ouvert/fermé | Notifications (`NotificationPort`, routeur SMTP/log), import (`ProfileImportPort`, stratégies CSV/JSON/Doctolib), fraude (`FraudDetectionPort` → règles puis IA), stockage (`DocumentStoragePort` local/S3) : on ajoute un adapteur sans modifier les cas d'usage. |
| **L** — Substitution | Tous les adapteurs implémentent des ports ; `NotificationRouter` comme les adapteurs factices sont substituables sans casser les services. |
| **I** — Ségrégation des interfaces | Ports fins : `PatientProfileRepository`, `RefreshTokenRepository`, `JwtTokenPort`, `TotpPort`, `CipherPort`… aucune classe ne dépend de méthodes inutilisées. |
| **D** — Inversion de dépendances | La couche applicative définit les ports (JWT, hachage, référentiel médical, KYC…) ; les détails techniques (Nimbus, BCrypt, HTTP) en dépendent. |

## 3. Design patterns présents

- **Ports & Adapters** (hexagonal) sur tout le module.
- **Value Object** : `Email`, `PhoneNumber` (validation/canonicalisation au constructeur).
- **Factory / agrégat riche** : fabriques `User.register`, `User.fromSocialProvider`,
  `OtpChallenge.issue`, `UserSession.start`, `Delegation.grant` ; règles de verrouillage
  et de cycle de vie encapsulées dans `User`.
- **Strategy** : `ProfileImportPort` (CSV…), `FraudDetectionPort`, `NotificationPort`,
  `DocumentStoragePort`, `IdentityVerificationPort`.
- **Observer / événements de domaine** : `DomainEventPublisher` → événements
  (`PatientRegistered`, `PractitionerApplicationSubmitted`, `SuspiciousLoginDetected`)
  écoutés par `AdminNotificationListener` (emails/SIEM/analytics ajoutables sans toucher au flux).
- **Anti-Corruption Layer** : adapteurs d'API externes (RPPS/ADELI, banque, KYC, CSV).
- **Specification** : politique de mot de passe (`PasswordPolicy`), règles de fraude.
- **DTO + Mapper** : commandes/vues applicatives, `ViewMapper`, mappers de persistance
  (le domaine ne fuit jamais vers le web ni vers JPA).
- **Builder** : revendications JWT (Nimbus `JWTClaimsSet`).
- **Template léger** : `OncePerRequestFilter` (JWT, rate limit).

## 4. Sécurité — flux

1. **Inscription** : validation des VOs → politique de mot de passe → unicité →
   hachage **BCrypt (cost 11)** → OTP (code 6 chiffres haché SHA-256, 5 min, 3 essais,
   anti-renvoi 60 s) → activation ou validation manuelle.
2. **Connexion** : échecs comptés, verrouillage 15 min après 5 tentatives ;
   MFA TOTP si activée ; sinon scoring de fraude → OTP step-up (nouvel appareil/pays).
3. **Jetons** : access token **JWT HS256** 15 min ; refresh token opaque (384 bits),
   haché en base, **rotation à chaque usage** ; réutilisation d'un jeton révoqué →
   révocation de toute la famille de sessions.
4. **Autorisation** : les autorités sont **recalculées à chaque requête**
   (rôles ∪ permissions directes ∪ délégations actives) ; la `tokenVersion`
   utilisateur permet d'invalider tous les JWT immédiatement.
5. **Chiffrement** : secret TOTP en colonne chiffrée AES-256-GCM
   (`EncryptedStringConverter`) ; IBAN et références de pièces **tokenisés** (HMAC,
   `tok_…`) ; masquage pour l'affichage.
6. **Traces** : `AuditLog` immuable (connexions, MFA, rôles, KYC, délégations,
   anonymisation) + `UserSession` (IP, User-Agent, pays/ville).

## 5. Couverture des exigences du Module 1

### 1.1 Inscription multi-profils

| Exigence | Réalisation |
|---|---|
| Patients : email/tél/social, OTP, CNI/passeport, mineur, import, KYC | `PatientRegistrationService`, `ContactVerificationService`, KYC (`KycDocumentsService`), `guardianUserId`, `DataImportService`, login social OAuth2 |
| Praticiens : validation manuelle, diplômes, RPPS/ADELI, RIB, RC pro, contrat e-, multi-établissements, remplaçants | `PractitionerRegistrationService`, `MedicalRegistryPort` (+stub), `BankAccountVerificationPort`, `PractitionerContract`, `EstablishmentMembership` (rôles OWNER/EMPLOYEE/REPLACER), validation admin |
| Secrétaires : compte lié, droits granulaires, multi-cabinets, audit | `SecretaryRegistrationService`, `User.directPermissions`, `supervisedPractitionerIds`, audit trail |
| Établissements : compte multi-praticiens, centralisé, facturation groupée, tableau de bord, départements | `EstablishmentRegistrationService`, `EstablishmentProfile.departments`, gestion des rattachements (permissions `billing.*` prêtes) |
| Administrateurs : super-admin, modérateurs, support tech/commercial, analystes | Rôles seeds + `AdminType`; endpoints `/admin/**` gardés par permissions ; super-admin amorcé |

### 1.2 Authentification & sécurité

| Exigence | Réalisation |
|---|---|
| 2FA/MFA | TOTP RFC 6238 (`TotpAdapter`, setup QR `otpauth://`) + OTP email step-up |
| Biométrie (empreinte/Face ID) | Côté terminal (WebAuthn/FIDO2) qui consomme le challenge MFA ; le port `TotpPort`/OTP couvre le second facteur. Un adapteur WebAuthn peut se brancher sur le même port de challenge. |
| SSO, OAuth 2.0 / OIDC | `spring-boot-starter-oauth2-client`, profil `social`, `SocialLoginSuccessHandler` (Google ; Keycloak/Azure/Okta en configuration) |
| JWT + refresh, sessions, détection connexions suspectes, blocage auto, historique IP/device/géoloc | `JwtTokenAdapter`, refresh rotatifs, `SessionService`, `RuleBasedFraudDetectionAdapter`, verrouillage, `UserSession` + `GeoLocationPort` |
| RBAC : rôles personnalisables, permissions granulaires, délégation temporaire, logs, révocation immédiate | `RoleAdministrationService`, permissions `module:action`, `DelegationService`, `AuditLog`, recalcul des autorités par requête + `tokenVersion` |
| Chiffrement E2E médical, tokenisation, anonymisation, IA fraude, WAF/DDoS, pentest, bug bounty | AES-GCM at rest (`EncryptedStringConverter`), `HmacTokenizationAdapter`, `AnalyticsQueryService` (anonymisation), moteur de règles de fraude remplaçable par une IA, `RateLimitFilter` + headers de sécurité. Le **WAF/DDoS**, la protection transport (TLS), les pentests et le bug bounty sont des mesures d'infrastructure/gouvernance (voir §6). |

## 6. Évolutions d'infrastructure (hors code applicatif)

- **WAF** : AWS WAF / Cloudflare devant l'API ; le `RateLimitFilter` est un garde-fou applicatif.
- **DDoS** : protection opérateur/CDN + scaling ; fenêtre fixe en mémoire ici (à remplacer par Redis en multi-instance).
- **Géolocalisation** : `UnknownGeoLocationAdapter` → brancher MaxMind/IPinfo via `GeoLocationPort`.
- **SMS** : adapter Orange SMS / Twilio sur `NotificationPort` ; **emails** : SMTP via variables `MAIL_*`.
- **Référentiels réels** : remplacer les stubs `StubMedicalRegistryAdapter`,
  `StubIdentityVerificationAdapter`, `StubBankAccountVerificationAdapter`.
- **Stockage KYC** : `LocalDocumentStorageAdapter` → S3/GCS via `DocumentStoragePort`.
- **Migrations** : pour la production, préférer Flyway/Liquibase à `ddl-auto=update`.

## 7. Règles de nommage des permissions

`module:action` — exemples : `iam.role.write`, `appointment.cancel`,
`billing.read`. Le super-admin reçoit automatiquement tout le catalogue ;
les rôles système sont définis dans `DataSeeder`, les rôles métier se créent
par l'API `/api/v1/admin/roles`.
