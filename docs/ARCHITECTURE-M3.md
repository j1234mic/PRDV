# Module 3 — Agenda & planning

## État de livraison

Backend Spring Boot / Java 17 en architecture hexagonale. Ce module n'est pas
encore l'intégralité du produit décrit : les limites restantes sont explicites
à la fin de ce document. Aucun fournisseur distant ni paiement n'est simulé
comme étant opérationnel.

## Architecture

- **Domaine pur** : `Agenda` immutable, `AgendaSettings`, `ConsultationReason`,
  `SchedulePattern`, `WaitingOffer`. Aucun import Spring/JPA.
- **Ports entrants** : `PlanningUseCase`, `PlanningInboxUseCase`, `PlanningMaintenanceUseCase`.
- **Ports sortants** : `AgendaStore`, `PlanningActor`, `PracticeSiteAccess`,
  `CalendarImportParser`, `PlanningMessageStore`, `PlanningNotificationGateway`.
- **Services applicatifs** : orchestration, contrôle d'appartenance, `Clock`
  injectée ; ni HTTP, ni JPA, ni fournisseur externe.
- **Adaptateurs** : REST/RBAC IAM, JPA, lieux d'exercice du module profils,
  import ICS strict, SMTP optionnel, outbox et tâches planifiées.
- Patterns : Repository / Unit of Work, agrégat immutable, ports/adaptateurs,
  Transactional Outbox, Adapter et Strategy pour les transports/parsers.

### Persistance et concurrence

| Table | Rôle |
|---|---|
| `planning_agendas` | Un agrégat JSON par praticien, version optimiste |
| `planning_write_mutex` | Coordination transactionnelle des écritures entre instances |
| `planning_audit` | Journal append-only via l'application, sans contenu médical |
| `planning_notifications` | Boîte privée persistante et outbox SMTP, indexées |

Toutes les mutations prennent **d'abord** le verrou global en base, puis celui
de l'agenda. Le contrôle des RDV actifs du patient dans les autres agendas et
l'écriture sont dans la même transaction. Le report conserve le même identifiant
et ne crée ni annulation, ni nouveaux frais, ni remise à zéro du plafond.
Les nouvelles conditions d'annulation sont figées à la réservation ; les RDV
anciens sans snapshot utilisent les anciennes règles de l'agrégat lors de leur
première lecture/évolution.

**Compromis explicite** : le verrou global et le parcours des agrégats rendent le
contrôle inter-praticiens sûr mais limitent le débit. Avant montée en charge,
normaliser les réservations, indexer les intervalles et introduire des verrous
par patient avec ordre d'acquisition déterministe. Ne pas supprimer le verrou
sans mécanisme équivalent. Le singleton est créé au démarrage dans une transaction
courte, avec prise en compte de la course entre instances.

Les changements de capacité ne peuvent descendre sous les réservations et
propositions présentes. Le trajet doit rester compatible avec les créneaux déjà
matérialisés. Les modifications des durées ne réécrivent pas les RDV existants.

## Fonctions disponibles

### Agenda

- Types : normale, première visite, suivi, urgence, téléconsultation, domicile,
  acte technique ; réunion, administratif et absence non réservables.
- Durée par défaut/type/pathologie, multiplicateur 1–3 et tampons.
- Préavis, horizon, capacité, règles d'annulation et seuils d'absences.
- Créneaux multi-sites, vue consolidée ou filtrée et marge de trajet fixe.
- Génération hebdomadaire/mensuelle avec fuseau IANA et période bornée.
- Rejet des heures locales ambiguës/inexistantes ; mois sans jour 31 ignorés.
- Modèles nommés persistants : enregistrer, supprimer, appliquer à une période.
- Duplication d'une période avec décalage en jours UTC (pas en jours locaux).
- Fermeture exceptionnelle d'une plage libre, congés, pause ou réunion : retire
  les créneaux non référencés et crée le blocage, atomiquement. Si des RDV ou
  propositions y sont rattachés, la fermeture échoue et ne les annule pas.
- Contrôle d'appartenance des lieux via `PracticeLocationRepository` du module 2.

### Motifs et réservation

- Catalogue de motifs avec type, durée minimale, documents à apporter et
  identifiants des questions obligatoires ; consultation par le patient.
- Si un catalogue est défini, `intake.reason` doit être l'identifiant du motif.
  Sinon, le motif reste libre pour préserver les usages existants.
- Questionnaire obligatoire vérifié, bénéficiaire/motif/symptômes/canal bornés.
- Consentement téléconsultation explicite à la réservation et à l'acceptation.
- Statut nouveau/connu calculé depuis les consultations terminées chez ce
  praticien ; le booléen historique `firstVisit` n'est plus une source de vérité.
- Réservation, préconfirmation manuelle ou confirmation automatique ; détail
  privé, export ICS, annulation motivée, calcul de frais sans débit, report.
- Rejet des conflits du patient **entre praticiens**, pas seulement dans l'agenda.

### Liste d'attente et propositions

- Inscription quand aucune disponibilité ne correspond à l'intervalle/site/type.
- Priorité urgence déclarée, puis ancienneté ; attribution manuelle possible.
- `automaticOffer=true` autorise les propositions automatiques, **pas** une
  réservation définitive ou un consentement médical implicite.
- Une proposition réserve de la capacité au plus 15 minutes, sans dépasser le
  préavis de réservation. Seul son patient peut accepter ou refuser.
- L'acceptation revalide motif, consentement, capacité et conflits entre médecins
  sous le même verrou que la réservation.
- Au refus ou à l'expiration, l'entrée sort de la liste et le candidat suivant
  devient éligible. Le patient peut se réinscrire volontairement.
- Une tâche périodique expire les propositions et attribue les suivantes ; toute
  mutation de l'agenda réalise également cette réconciliation.
- L'acceptation répétée ne peut pas réserver deux fois : la proposition consommée
  n'existe plus, la seconde requête répond 404.

### File virtuelle et retards

- Check-in entre H−1 et fin du créneau, appel, clôture et no-show après la fin.
- Retard déclaré par lieu (0–480 minutes), notification persistante des patients.
- Positions et estimation séparées par site ; pas de cumul des files de cabinets
  distincts. Le patient ne voit que ses propres entrées.
- L'estimation reste déterministe, fondée sur les durées et le retard déclaré,
  sans déduction automatique du temps médical effectivement restant.

### Notifications et audit

- Audit et messages sont insérés **avec** la mutation métier, jamais après un
  commit séparé. Un échec métier ne doit créer ni message ni audit de succès.
- Boîte de notifications privée paginée et marquage lu avec vérification de
  propriété ; journal de l'agenda réservé à son praticien.
- Événements : réservation, report, statuts (dont appel), libération de créneau,
  proposition et retard. Aucun symptôme ni réponse médicale dans les messages.
- Les messages sont toujours disponibles dans l'application. Les événements
  patient de RDV/liste d'attente sont éligibles SMTP seulement lorsque le canal
  `EMAIL` a été demandé. Les alertes de retard et messages praticien restent
  in-app dans cette version.
- Worker SMTP désactivé par défaut ; refuse un SMTP non configuré au lieu de
  prétendre envoyer. Retry exponentiel, 8 tentatives puis état `DEAD`.
- SMTP est **at-least-once** : un arrêt après envoi et avant commit peut dupliquer
  un email. Le verrou outbox reste détenu pendant l'envoi d'un petit lot ; prévoir
  timeout SMTP et claim/lease avant déploiement à fort volume.

### Import / export de calendrier

- Export privé d'un RDV en ICS générique sans motif ni symptôme.
- Import de fichiers ICS en plages bloquées, sans importer de contenu clinique.
- Sous-ensemble supporté : `VEVENT`, `UID`, dates UTC explicites et `DTEND`.
- Les récurrences non développées, TZID, événements journée entière, durées
  implicites et composants inconnus sont rejetés, pas interprétés silencieusement.
- Limites : 1 million de caractères, 500 événements. Import identique idempotent ;
  événement modifié portant le même UID refusé (ce n'est pas une synchronisation).
- Tout le lot est refusé si un événement chevauche une plage existante.

## Configuration

```yaml
prdv:
  planning:
    waiting-offers:
      enabled: true
      poll-ms: 60000
    notifications:
      email-enabled: false
      poll-ms: 30000
      from: no-reply@prdv.app
```

Pour activer le transport réel : configurer `spring.mail.*`, des timeouts SMTP
finis et `prdv.planning.notifications.email-enabled=true`. Ne jamais committer de
mot de passe SMTP. Les jobs peuvent être désactivés dans les tests.

## Sécurité

- Gestion d'agenda/catalogue/modèles : `ROLE_PRACTITIONER`, toujours son propre
  agenda (identité IAM, jamais un propriétaire envoyé dans le corps).
- Réservation et inscription : `ROLE_PATIENT`.
- RDV : lecture/mutation par son patient ou son praticien ; appel/confirmation/
  clôture/no-show réservés au praticien ; patient autorisé au check-in.
- Notifications et propositions : accès exclusivement au propriétaire ; seules
  les offres de son agenda sont visibles au praticien.
- Les identifiants utilisateur ne sont pas interprétés comme des identifiants
  de profil ; les lieux doivent exister et appartenir à ce praticien.

## Tests et vérifications

| Niveau | Classes / commande | Périmètre |
|---|---|---|
| Domaine | `AgendaTest`, `AgendaManagementTest`, `SchedulePatternTest` | politiques, durées, immutabilité, motifs, clôture, report, offres et expiration |
| Application | `PlanningServiceTest`, `PlanningMaintenanceServiceTest` | isolation, transitions, report atomique et maintenance des propositions |
| Adaptateur calendrier | `IcsCalendarImportAdapterTest` | import strict, UID stable, UTC, rejet des récurrences/dates invalides |
| Outbox | `PlanningOutboxDispatcherTest` | succès, retry et dead-letter |
| Intégration | `JpaAgendaStoreTest` | JSON, rollback, audit/outbox et concurrence dans un agenda et entre praticiens |
| HTTP E2E | `PlanningApiE2ETest` | JWT réels, RBAC, réservation, motifs/templates, proposition privée, acceptation, notifications |
| Sans framework | `tools/planning/PlanningSmoke.java` | 15 contrôles exécutables sans Maven |

- `mvn -B verify` : toutes les suites JUnit, intégration et E2E (Surefire).
- `docs/workflows/planning-ci.yml` : modèle de build complet H2 Java 17 et de job
  JPA/concurrence contre MySQL 8.4. À copier vers `.github/workflows/ci.yml`
  avec une connexion disposant de la permission `workflows` : la connexion GitHub
  de cette session refuse la publication de workflows actifs.
  **CI non activée et non exécutée dans cette session**.
- Sandbox : domaine et services applicatifs compilés avec un javac portable
  Java 23, smoke exécuté sur runtime Java 25 : **15 contrôles réussis**.
- Analyse syntaxique et références croisées sans erreur.
- **Build Maven cible Java 17 et tests Spring/JPA/E2E non exécutés localement** :
  Maven/JDK standard et dépendances Maven ne sont pas disponibles dans le sandbox.
  La compilation portable ne valide ni Spring, ni JPQL, ni JPA.

## Travaux encore nécessaires — ne pas annoncer le module complet

1. Interface utilisateur et parcours praticien depuis recherche/favoris/historique/
   recommandations ; chatbot de choix du motif.
2. Calendriers externes bidirectionnels Google, Outlook/Exchange, Apple/CalDAV :
   OAuth, secrets chiffrés, webhooks, curseurs, conflits/idempotence et tests réels.
3. Paiement, encaissement des pénalités, itinéraires et partage avec un proche.
4. Bénéficiaires enfants/proches avec autorisation : le champ bénéficiaire actuel
   est descriptif, ce n'est pas une délégation validée ni une identité indépendante.
5. Gestion par secrétaires/délégations et calcul routier des trajets.
6. Notifications SMS/push et temps réel ; préférences globales du module profils ;
   outil d'exploitation des messages `DEAD` et redélivrance contrôlée.
7. Contact urgent, report automatique de RDV déjà existants et triage médical
   de l'urgence ; la liste d'attente utilise une urgence déclarative.
8. RGPD : raccordement export/effacement du contexte profils, rétention/anonymisation
   de l'historique et chiffrement applicatif des données de réservation.
9. Normalisation/indexation/pagination de l'historique d'agenda, migrations
   versionnées et validation des suites Java 17 / MySQL avant fusion.
10. Raccordement de `TreatingPractitionersPort` après revue de confidentialité :
    l'accès aux dossiers médicaux n'est pas élargi implicitement par ce lot.

Pour les agrégats JSON créés avant ce complément, `settings` absent est remplacé
par les valeurs par défaut. Aucun script ne supprime les données existantes.
La migration vers une production avec schéma versionné reste à prévoir.
