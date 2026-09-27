# Architecture du Module 2 (Profils & gestion des données)

Contexte borné `com.prdv.rdv.profile`, livré dans la même application Spring Boot
que le module 1 (`com.prdv.rdv.iam`) mais **strictement découplé** : le module 2
ne connaît le module 1 qu'à travers ses **ports sortants**
(`adapter/out/iam/*`), jamais par appel direct à ses services.

## 1. Architecture hexagonale

```
                          ┌───────────────────────────────────────────────┐
   Adaptateurs entrants   │                  APPLICATION                  │   Adaptateurs sortants
┌──────────────────────┐  │                                               │ ┌──────────────────────────┐
│ REST (8 contrôleurs) │──▶  ports entrants (13 cas d'usage)               │ │ JPA/H2/MySQL (10 adapteurs)│
│  /api/v1/profile     │  │  command (6 holders) ──▶ service (13)         │ │ pont IAM (7 adapteurs)    │
│  /api/v1/medical-…   │  │  result  (ProfileViews) ◀── support (5)       │ │ stockage local            │
│  /api/v1/documents   │  │                                               │ │ classification + OCR      │
│  /api/v1/health      │  │            DOMAINE (pur)                      │ │ DMP, Apple Health, Fit    │
│  /api/v1/privacy     │  │  identity · medical · document · health       │ │ géocodage, alertes        │
│  /api/v1/practitioner│  │  preference · practitioner                    │ │ export RGPD               │
│  /api/v1/locations   │  │  invariants, Value Objects, événements        │ │ événements Spring         │
│  /api/v1/directory   │  │                                               │ └──────────────────────────┘
└──────────────────────┘  └───────────────────────────────────────────────┘
                                          ports sortants (26)
```

Règles respectées :

- le **domaine** n'importe ni Spring MVC, ni JPA, ni servlet (seule concession :
  `HttpStatus` dans le catalogue d'erreurs, comme en module 1) ;
- les **entités JPA** sont séparées des agrégats : 4 mappers assurent la
  traduction dans les deux sens ;
- les **vues** renvoyées au web (`ProfileViews.*`) sont des enregistrements
  immuables : aucune entité ni agrégat ne franchit la frontière applicative ;
- les **données sensibles** (NIR, IBAN) sont tokenisées + masquées : la valeur
  brute n'est jamais persistée ni renvoyée.

## 2. SOLID en pratique

| Principe | Mise en œuvre dans le module 2 |
|---|---|
| **S**ingle responsibility | 13 services = 13 cas d'usage ; la garde d'accès (`ProfileAccessGuard`), l'audit (`ProfileAuditTrail`), l'assemblage des vues (`ProfileViewMapper`), l'export (`ProfileDataAssembler`) et l'évaluation des alertes (`HealthAlertEvaluator`) sont des collaborateurs dédiés |
| **O**pen/closed | `HealthAlertEvaluator` reçoit ses règles (seuils par défaut ou fournis) ; `BadgePolicy` est une politique remplaçable ; `ProfileDomainConfig.defaultRules()` est un point d'extension ; chaque moteur IA/OCR/DMP est un port |
| **L**iskov | tout port sortant a plusieurs implémentations interchangeables (ex. `DocumentClassificationPort` : `KeywordDocumentClassificationAdapter` aujourd'hui, un modèle entraîné demain) sans modifier les cas d'usage |
| **I**nterface segregation | 13 ports entrants fins (lecture séparée de l'écriture : `MedicalRecordQueryUseCase` / `MedicalRecordCommandUseCase`) ; 26 ports sortants à 1–4 méthodes |
| **D**ependency inversion | les services dépendent de ports, jamais d'implémentations ; le pont vers IAM est inversé (le module 2 déclare `CurrentUserPort`, `SensitiveDataPort`, `AccountErasurePort`… que des adaptateurs `iam` réalisent) |

## 3. Design patterns présents

| Pattern | Où |
|---|---|
| **Ports & Adapters** | `application/port/{input,output}` ↔ `adapter/{in,out}` |
| **Repository** | 10 ports `*Repository` + 10 adapteurs JPA |
| **Value Object** | `CivilStatus`, `PostalAddress`, `VitaleCard`, `BodyMetrics`, `BloodGroup`, `RatingSummary`, `Badge`, `Tariff`, `Siret`… |
| **Aggregate root** | `PatientIdentity`, `MedicalRecord`, `MedicalDocument`, `PractitionerDossier`, `PrivacyPreferences` |
| **Factory method** | `MedicalDocument.upload(...)`, `MedicalRecord.open(...)`, `PractitionerDossier.create(...)`, `PrivacyPreferences.defaults(...)`, `HealthAlert.raise(...)` |
| **Strategy** | classification de documents, OCR, fournisseurs d'objets connectés (`AppleHealthProviderAdapter` / `GoogleFitProviderAdapter`), export (`DataExportPort`), règles d'alerte |
| **Anti-Corruption Layer** | `StubDmpGatewayAdapter`, `AppleHealthProviderAdapter`, `GoogleFitProviderAdapter`, `UnknownAddressGeocodingAdapter`, pont `adapter/out/iam/*` |
| **Observer / Domain events** | `ProfileEvent` (13 événements) publiés par `SpringProfileEventPublisher`, consommés par `ProfileEventListener` |
| **Mapper (traduction de couches)** | 4 mappers de persistance + `ProfileViewMapper` |
| **Command** | 6 holders de commandes immuables (`PatientIdentityCommands`, `MedicalRecordCommands`, `DocumentCommands`, `HealthCommands`, `PrivacyCommands`, `PractitionerProfileCommands`) |
| **Template method** | `MedicalRecordCommandService.apply(...)` : contrôle d'accès → règle métier → persistance → audit → événement |
| **Specification (dérivée)** | `MetricAlertRule.isSatisfiedBy(metric)` et `BadgePolicy.evaluate(...)` |
| **Guard / Policy** | `ProfileAccessGuard` (granularité), `BadgePolicy` (badges), `MetricAlertRule` (seuils) |
| **Builder-less immutability** | les vues et les commandes sont des `record` |

## 4. Sécurité & confidentialité

| Sujet | Réalisation |
|---|---|
| Autorisations | `@PreAuthorize("hasAuthority('profile.…')")` sur chaque endpoint ; 16 permissions du module ajoutées au catalogue (`DataSeeder`) et affectées aux rôles |
| Annuaire public | `GET /api/v1/directory/**` en `permitAll` ; la notation d'un praticien reste authentifiée (seul `GET` est ouvert) |
| Contrôle d'accès aux données de santé | `ProfileAccessGuard.requireAccess(...)` applique les règles de granularité du patient avant toute lecture croisée |
| Données sensibles | NIR et IBAN **tokenisés + masqués** (`SensitiveDataPort`), valeur brute jamais persistée ; pièce d'identité stockée hors base, accès tracé |
| Consentements | preuve conservée (finalité, version de politique, IP, horodatage), historique append-only, retrait daté |
| Effacement RGPD | `DataPortabilityService.requestErasure` : purge ordonnée (identité → dossier → fichiers → objets/mesures/alertes → préférences → dossier pro → lieux → avis) puis anonymisation du compte côté IAM |
| Traçabilité | 14 actions d'audit du module mappées sur `AuditLog.Action` du module 1 |

## 5. Couverture des exigences du Module 2

### 2.1 Profil patient complet

| Exigence | Réalisation |
|---|---|
| État civil complet | `PatientIdentity.CivilStatus` (civilité, nom d'usage, nom de naissance, prénom usuel, naissance, genre, situation, nationalité) |
| Contacts multiples (fixe, mobile, pro) | `PatientIdentity.ContactPoint` (`ContactType` MOBILE/HOME/WORK/FAX/EMAIL, favori) — `PUT /profile/contacts` |
| Adresses multiples (domicile, travail) | `PatientIdentity.PostalAddress` (`AddressType` HOME/WORK/OTHER + géolocalisation + adresse par défaut) — `PUT /profile/addresses` |
| Personne à prévenir (urgence) | `PatientIdentity.EmergencyContact` — `PUT /profile/emergency-contact` |
| Médecin traitant déclaré | `PatientIdentity.TreatingPhysician` (+ déclaration à l'assurance) — `PUT /profile/treating-physician` |
| N° sécurité sociale | `SocialSecurityNumber` (NIR validé), **token + masque** seuls persistés — `POST /profile/social-security-number` |
| Mutuelle principale + complémentaire | `PatientIdentity.HealthInsurance` (`InsuranceType` : principale, complémentaire, CMU, AME, privée, étrangère) — `PUT /profile/insurances` |
| Photo de profil | `LocalMedicalFileStorageAdapter` + `POST /profile/photo` (multipart, limite `prdv.profile.media.max-photo-bytes`) |
| Pièce d'identité (stockage sécurisé) | stockage hors base + `identityDocumentToken`, accès audité — `POST /profile/identity-document` |
| Carte Vitale (scan + NFC) | `PatientIdentity.VitaleCard` (`VitaleReadMode` NFC/SCAN/MANUAL), NIR tokenisé, scan stocké — `POST /profile/vitale-card` |
| Partage avec DMP national | `DmpGatewayPort` (+ passerelle de simulation), consentement `DMP_SHARING`, `POST /profile/dmp/link`, `PUT /profile/dmp/sharing`, `POST /profile/dmp/sync` |
| Antécédents médicaux structurés | `MedicalRecord.MedicalHistoryEntry` (code, statut ACTIVE/RESOLVED/MONITORED, source) |
| Antécédents familiaux | `MedicalRecord.FamilyHistoryEntry` (`FamilyRelation`, âge au diagnostic) |
| Allergies / intolérances | `MedicalRecord.Allergy` (`AllergenType`, `AllergySeverity`, résolution sans suppression) |
| Vaccinations avec rappels | `MedicalRecord.Vaccination` + `VaccinationReminderView` (fenêtre `reminder-lookahead-days`) — `GET /medical-record/{id}/vaccination-reminders` |
| Groupe sanguin | `BloodGroup` (ABO + Rh, parsing `A+`, `O-`…) |
| Maladies chroniques | `MedicalRecord.ChronicCondition` (affection de longue durée) |
| Traitements en cours | `MedicalRecord.Treatment` (début/fin, prescripteur, indication) |
| Chirurgies passées | `MedicalRecord.Surgery` |
| Hospitalisations | `MedicalRecord.Hospitalization` (+ durée de séjour calculée) |
| IMC et constantes vitales | `VitalSigns` (TA, FC, SpO₂, température, FR) + `BodyMetrics.bmi()` et catégories OMS |
| Handicaps / limitations | `MedicalRecord.Disability` (`DisabilityType`, taux, aides) |
| Upload multi-format (PDF, JPG, DICOM) | `POST /documents` (multipart, 25 Mo), métadonnées DICOM (`modality`, `studyDescription`) |
| Classification automatique par IA | `DocumentClassificationPort` + `KeywordDocumentClassificationAdapter`, requalification manuelle `PUT /documents/{id}/category` |
| OCR / extraction | `OcrExtractionPort` + `TextOcrExtractionAdapter`, `MedicalDocument.OcrResult` (texte brut + champs extraits) |
| Ordonnances, analyses, imagerie, CR opératoires, courriers, certificats | `MedicalDocument.DocumentCategory` (PRESCRIPTION, LAB_RESULT, IMAGING_XRAY/MRI/CT_SCAN/ULTRASOUND, OPERATIVE_REPORT, HOSPITALIZATION_REPORT, CORRESPONDENCE, MEDICAL_CERTIFICATE, VACCINATION_RECORD, OTHER) |
| Versioning des documents | `DocumentVersion` append-only, `currentVersion()`, document archivé immutable |
| Partage sécurisé avec praticiens | `DocumentShare` (permission VIEW / VIEW_AND_DOWNLOAD, motif, expiration, révocation tracée), table dédiée indexée |
| Apple Health / Google Fit | `ConnectedDeviceProviderPort` + `AppleHealthProviderAdapter`, `GoogleFitProviderAdapter` |
| Montres, tensiomètre, glucomètre, balance, oxymètre, ECG portable | `ConnectedDevice.DeviceType` (WEARABLE_WATCH, BLOOD_PRESSURE_MONITOR, GLUCOMETER, SMART_SCALE, PULSE_OXIMETER, PORTABLE_ECG, THERMOMETER) + `BLUETOOTH_SIG` |
| Graphiques d'évolution | `HealthMetricsQueryUseCase.series(...)` → `MetricSeriesView` (agrégation HOUR/DAY/WEEK + statistiques) |
| Alertes automatiques | `HealthAlertEvaluator` + `MetricAlertRule.defaultRules()` (SpO₂, TA, glycémie, FC, température, FR, ECG), `HealthAlertNotifierPort` |
| Langues | `PrivacyPreferences.updateLanguages(...)` (langue principale parmi les préférées) |
| Accessibilité (malvoyant, sourd) | `AccessibilitySettings` (`AccessibilityNeed` VISUALLY_IMPAIRED/HEARING_IMPAIRED/…, lecteur d'écran, gros caractères, LSF, sous-titres) |
| Préférences de communication | `CommunicationSettings` (canaux + plages de tranquillité) |
| Consentement RGPD | `ConsentRecord` (finalité, version de politique, IP, horodatage, retrait), 6 finalités |
| Granularité « qui peut voir quoi » | `VisibilityRule` par `DataCategory` × `VisibilityLevel` (PRIVATE / MY_PRACTITIONERS / SPECIFIC_PRACTITIONERS / ALL_PRACTITIONERS) |
| Export de données (RGPD) | `DataPortabilityUseCase.exportMyData` → `ProfileDataAssembler` + `JsonDataExportAdapter` — `POST /privacy/export` |
| Suppression de compte | `DataPortabilityUseCase.requestErasure` → purge + révocation globale + anonymisation IAM — `DELETE /privacy/account` |

### 2.2 Profil médecin / praticien professionnel

| Exigence | Réalisation |
|---|---|
| Titre / civilité | `PractitionerDossier.Title` (DOCTOR, PROFESSOR, MISTER, MISS, MRS) |
| RPPS / ADELI | `ProfessionalIdentity.rppsNumber/adeliNumber` (au moins un des deux) |
| Ordre d'inscription | `RegistrationOrder` (médecins, chirurgiens-dentistes, sages-femmes, pharmaciens, infirmiers, kinés, pédicures, orthophonistes, orthoptistes…) |
| Spécialités principale / secondaires | `mainSpecialty` + `secondarySpecialties` |
| Sous-spécialités, compétences | `subSpecialties`, `skills` |
| Diplômes / formations | `PractitionerDossier.Diploma` (libellé, établissement, année, spécialité) |
| Années d'expérience, langues | `experienceYears`, `languages` |
| Photo pro, vidéo de présentation | `POST /practitioner-dossier/photo`, `POST /practitioner-dossier/presentation-video` (clés conservées lors des mises à jour de l'identité) |
| Multi-lieux d'exercice | `PracticeLocation` (lieu principal explicite, `POST/PUT/DELETE /locations`) |
| Adresse + géolocalisation | `PracticeLocation.latitude/longitude` + `GeocodingPort` |
| Photos du cabinet, visite virtuelle 360° | `PracticeLocation.Photo` (`PhotoType` salle d'attente, salle de consultation, équipement, extérieur, accueil) + `virtualTourUrl` |
| Horaires | `PracticeLocation.OpeningHours` (jour, ouverture, fermeture, fermé, note) |
| Téléphone fixe/mobile, fax, email pro, site web, réseaux sociaux | champs dédiés + `socialLinks` (map) |
| Accessibilité handicapés, parking, transports | `wheelchairAccessible` (filtre d'annuaire), `parkingAvailable`, `publicTransportInfo` |
| Secteur de convention (1, 2, 3) | `ConventionSector` SECTOR_1/2/3 |
| Tarifs par type de consultation | `PractitionerDossier.Tariff` (acte, montant, devise, prise en charge) |
| OPTAM / OPTAM-CO | `optam`, `optamCo`, `optamSignedOn` |
| Modes de paiement | `PaymentMethod` (espèces, chèque, CB, virement, tiers-payant) |
| Tiers-payant (conditions) | `thirdPartyPayment` + `thirdPartyPaymentConditions` |
| Actes pratiqués (nomenclature) | `actsPerformed` |
| Équipements, pathologies traitées, âges acceptés | `availableEquipment`, `treatedPathologies`, `AgeRange` |
| Téléconsultation | `teleconsultation` + plateforme (filtre d'annuaire) |
| SIRET | `Siret.of(...)` (format + contrôle) |
| RIB pro | IBAN reçu → **token + masque** uniquement (`SensitiveDataPort`) |
| Assurance RC Pro, URSSAF, AGA/CGA, cotisation ordinale | `ProfessionalManagement` (assureur, police, échéance, URSSAF, AGA/CGA, année et montant de cotisation) |
| Certifications qualité, accréditations | `qualityCertifications`, `Accreditation` (organisme, référence, validité) |
| Réseau professionnel | `PractitionerDossier.NetworkContact` (`NetworkRole` : CORRESPONDENT, SPECIALIST, LABORATORY, PHARMACY, IMAGING_CENTER, HOSPITAL) |
| Description longue, domaines d'expertise | `VisibilityProfile.longDescription`, `expertiseDomains` |
| Publications / recherches, distinctions, conférences, médias | `publications`, `distinctions`, `conferenceTalks`, `mediaReferences` |
| Avis et notes patients | `PractitionerRating` (1–5, commentaire, modération) + `RatingSummary` calculée à la volée ; **un avis par patient** |
| Badges « vérifié », « populaire », « nouveau » | `BadgePolicy.evaluate(...)` : vérifié = identité + diplômes + assurance + RIB ; populaire = ≥ 20 avis et ≥ 4,5/5 ; nouveau = < 90 jours. **Dérivés, jamais stockés** |
| Visibilité publique | `GET /api/v1/directory/**` sans authentification ; `isPublishable()` (spécialité + secteur) requis pour apparaître |

## 6. Persistance

| Choix | Justification |
|---|---|
| Une table par agrégat « volumineux ou interrogeable » | mesures de santé, alertes, objets connectés, documents, partages, lieux, avis, dossiers pro |
| Colonnes JSON (`@Convert`) pour les collections de Value Objects | antécédents, allergies, vaccins, traitements, consentements, règles de visibilité : toujours lus/écrits en bloc, jamais filtrés en SQL |
| `profile_document_shares` en table dédiée | le contrôle d'accès et « documents partagés avec moi » sont des requêtes indexées et paginées |
| Pas de `@PrePersist` sur `ProfileEntity` | les horodatages sont décidés par le domaine (`Clock` injecté), donc reproductibles en test |
| Mutuelles en liste JSON | évite la collision de colonnes d'un embeddable répété (principale = `PRINCIPAL`) |

## 7. Configuration

```yaml
prdv:
  profile:
    documents: { max-bytes: 26214400, folder: medical-documents, reminder-lookahead-days: 60 }
    media:     { max-photo-bytes: 5242880, max-video-bytes: 104857600 }
    health:    { default-sync-window-days: 30, max-metrics-per-query: 2000, max-metrics-per-batch: 500 }
    consent:   { policy-version: "2026-09" }
    dmp:       { enabled: false }   # true = une passerelle réelle doit être fournie
    storage:   { local-path: ./storage/profile }
```

Le stockage local du module 2 est distinct de celui du module 1 :
`prdv.profile.storage.local-path` (défaut `./storage/profile`), avec des
sous-dossiers par usage (`medical-documents/`, `patient-media/`,
`practitioner-media/`, `practice-locations/`). L'adaptateur refuse toute clé qui
sortirait de cette racine (garde anti-traversée de chemin).

## 8. Limites assumées (à brancher en production)

- **DMP** : la passerelle livrée simule et journalise les échanges
  (`prdv.profile.dmp.enabled=false`) ; un adapteur CPS réel s'implémente sur
  `DmpGatewayPort` sans toucher au domaine.
- **Objets connectés** : Apple Health / Google Fit sont des squelettes
  d'intégration (import par lot fonctionnel, synchronisation distante à brancher).
- **Classification / OCR** : moteur par mots-clés et extraction texte ; un modèle
  entraîné remplace `DocumentClassificationPort` / `OcrExtractionPort`.
- **Praticiens qui suivent un patient** : `TreatatingPractitionersAdapter`
  renvoie un ensemble vide tant que le module 3 (rendez-vous) n'est pas livré —
  la visibilité `MY_PRACTITIONERS` reste donc fermée par défaut (choix sûr).
- **Géocodage** : `UnknownAddressGeocodingAdapter` ne devine pas de coordonnées.
