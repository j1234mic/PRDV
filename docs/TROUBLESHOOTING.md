# Dépannage — PRDV

## `Feature not supported: "AUTO_SERVER=TRUE && DB_CLOSE_ON_EXIT=FALSE"` (H2 `[50100]`)

**Stack trace typique :**

```
WARN  o.h.engine.jdbc.spi.SqlExceptionHelper : SQL Error: 50100, SQLState: HYC00
ERROR o.h.engine.jdbc.spi.SqlExceptionHelper : Fonctionnalité non supportée: "AUTO_SERVER=TRUE && DB_CLOSE_ON_EXIT=FALSE"
Caused by: org.h2.jdbc.JdbcSQLFeatureNotSupportedException: Feature not supported [50100-224]
...
Failed to initialize JPA EntityManagerFactory: Unable to build Hibernate SessionFactory
Error creating bean with name 'securityConfig' ... Cannot resolve reference to bean 'entityManagerFactory'
```

### Cause

H2 2.x interdit deux réglages contradictoires dans la même URL JDBC :

| Paramètre | Effet |
|---|---|
| `AUTO_SERVER=TRUE` | mode « mixed/auto-server » : le premier processus ouvre un serveur TCP et les autres s'y connectent — H2 doit pouvoir fermer proprement la base à l'arrêt du JVM |
| `DB_CLOSE_ON_EXIT=FALSE` | désactive justement le shutdown hook de H2 |

D'où l'erreur `50100 / HYC00`. Rien à voir avec MySQL : le pilote refuse d'ouvrir la
base, donc Hibernate ne peut pas construire la `SessionFactory`, donc tous les beans
JPA (`userJpaRepository`, `userPersistenceAdapter`, `jwtAuthenticationFilter`,
`securityConfig`…) échouent en cascade et Tomcat ne démarre pas.

### Solutions

#### Solution 1 — URL par défaut du dépôt (recommandée)

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/prdv;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
```

```bash
git pull
mvn spring-boot:run
```

La console H2 intégrée (http://localhost:8080/h2-console, URL `jdbc:h2:file:./data/prdv`,
user `sa`) fonctionne **sans** `AUTO_SERVER` : elle tourne dans le même JVM et se
rattache à la base déjà ouverte par Hikari.

#### Solution 2 — Accéder au fichier depuis DBeaver / IntelliJ pendant que l'app tourne

Activez le mixed mode **en retirant** `DB_CLOSE_ON_EXIT=FALSE` :

```bash
SPRING_DATASOURCE_URL='jdbc:h2:file:./data/prdv;MODE=MySQL;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE' mvn spring-boot:run
```

H2 écrit alors l'adresse du serveur dans `./data/prdv.lock.db` ; les autres processus
(utilisez exactement la même URL) s'y connectent automatiquement. Un port TCP aléatoire
est ouvert en local le temps de l'exécution.

#### Solution 3 — Base en mémoire (aucun fichier, aucun verrou)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Vérifier les surcharges locales

L'URL peut être réinjectée par (par ordre de priorité décroissante) :

```bash
env | grep -i datasource          # SPRING_DATASOURCE_URL
cat ~/.m2/settings.xml            # profils Maven
ls src/main/resources/application-*.yml   # profil actif non versionné
```

Un test de non-régression couvre ce cas :
`src/test/java/com/prdv/rdv/common/config/H2DatasourceUrlTest.java`
(vérifie l'URL de `application.yml` **et** ouvre réellement une base H2 fichier avec
ces réglages — une base `mem:` ignorerait `AUTO_SERVER` et ne détecterait rien).

## `Valeur non permise pour la colonne` (H2 `22030`) à l'insertion dans `audit_logs`

**Message typique :**

```
Valeur non permise pour la colonne "('USER_REGISTERED', 'OTP_REQUESTED', ..., 'SOCIAL_LOGIN')": "PROFILE_UPDATED"
insert into audit_logs (action,created_at,detail,ip_address,outcome,resource_id,resource_type,user_id,id) values (...) [22030-224]
```

### Cause

Hibernate crée les attributs `@Enumerated(EnumType.STRING)` en colonnes `ENUM` natives sur H2 et sur MySQL : la liste des valeurs acceptées est figée à la création de la table.

La base `./data/prdv` a été créée avant l'ajout des actions du module 2 : `audit_logs.action` n'acceptait que les 25 valeurs du module 1 (de `USER_REGISTERED` à `SOCIAL_LOGIN`). L'action `PROFILE_UPDATED`, écrite par le module profil, est donc refusée.

`ddl-auto: update` ajoute les tables et colonnes manquantes, mais **ne modifie jamais une colonne existante** : sans correction, redémarrer l'application ne suffit pas. Le même blocage touche toute colonne `ENUM` dont l'énumération Java a été étendue après la création de la table.

### Solutions

#### Solution 1 — Correction automatique au démarrage (recommandée)

Depuis cette version, `EnumColumnReconciler` (`src/main/java/com/prdv/rdv/common/persistence/`) convertit **toutes** les colonnes `ENUM` du schéma en `VARCHAR(255)` au démarrage, juste après le `ddl-auto` de Hibernate et avant l'ouverture du serveur HTTP :

```bash
git pull
mvn spring-boot:run
```

Le journal confirme la conversion :

```
WARN  c.p.r.c.persistence.EnumColumnReconciler : Colonnes ENUM converties en VARCHAR(255) : [AUDIT_LOGS.ACTION, AUDIT_LOGS.OUTCOME, KYC_DOCUMENTS.STATUS, ...]
```

- **Sans perte** : les lignes existantes, la contrainte `NOT NULL` et les index sont conservés.
- **Idempotente** : aux démarrages suivants, plus aucune colonne n'est détectée.
- **Conditionnée au mode du schéma** : elle n'agit que si `spring.jpa.hibernate.ddl-auto` vaut `update`, `create` ou `create-drop`. Avec `none` ou `validate`, l'application ne modifie jamais le schéma.

#### Solution 2 — Schéma géré hors Hibernate (`ddl-auto: none` ou `validate`)

Listez les colonnes `ENUM`, puis convertissez-les avec votre outil de migration :

```sql
-- MySQL
SELECT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND DATA_TYPE = 'enum';
ALTER TABLE audit_logs MODIFY COLUMN action VARCHAR(255) NULL;

-- H2 (conserve la nullabilité)
SELECT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
 WHERE TABLE_SCHEMA = 'PUBLIC' AND DATA_TYPE = 'ENUM';
ALTER TABLE audit_logs ALTER COLUMN action SET DATA TYPE VARCHAR(255);
```

Répétez l'opération pour chaque colonne listée.

#### Solution 3 — Repartir d'une base vide (développement uniquement)

Les comptes et données créés localement sont alors perdus :

```bash
rm -rf ./data
mvn spring-boot:run
```

### Tests de non-régression

- `src/test/java/com/prdv/rdv/common/persistence/EnumColumnMigrationTest.java` : schéma legacy à 25 valeurs ; `PROFILE_UPDATED` et toutes les actions acceptées après conversion, données, `NOT NULL` et index conservés, idempotence.
- `src/test/java/com/prdv/rdv/common/persistence/EnumColumnReconcilerTest.java` : conversion uniquement lorsque `ddl-auto` est géré par Hibernate.

## `Communications link failure` / `Connexion refusée` / `Unable to open JDBC Connection`

**Stack trace typique :**

```
WARN  o.h.engine.jdbc.spi.SqlExceptionHelper : SQL Error: 0, SQLState: 08S01
ERROR o.h.engine.jdbc.spi.SqlExceptionHelper : Communications link failure
Caused by: java.net.ConnectException: Connexion refusée
...
Failed to initialize JPA EntityManagerFactory: Unable to build Hibernate SessionFactory
Error creating bean with name 'securityConfig' ... Cannot resolve reference to bean 'entityManagerFactory'
```

### Cause

L'application tente de se connecter à MySQL (`jdbc:mysql://localhost:3306/prdv`) mais :

- MySQL n'est pas démarré, ou
- Le port 3306 est occupé par un autre MySQL natif, ou
- Les variables `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD` sont incorrectes.

Depuis la version avec support H2, le **profil par défaut est H2 fichier** (`./data/prdv.mv.db`), donc `mvn spring-boot:run` fonctionne sans Docker. Si vous forcez le profil `mysql`, MySQL devient requis.

### Solutions

#### Solution 1 — Utiliser H2 embarquée (recommandé pour dev rapide, par défaut)

```bash
# Par défaut, H2 fichier, sans Docker
mvn spring-boot:run

# Ou explicitement H2 mémoire (éphémère)
mvn spring-boot:run -Dspring-boot.run.profiles=h2

# Ou H2 mémoire + logs SQL verbeux
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- Fichier créé (défaut) : `./data/prdv.mv.db`
- Console H2 : http://localhost:8080/h2-console
  - Défaut : JDBC URL `jdbc:h2:file:./data/prdv`, user `sa`
  - Mémoire : JDBC URL `jdbc:h2:mem:prdv`, user `sa`, pas de mot de passe
- Swagger UI : http://localhost:8080/swagger-ui.html

Combinaison avec OAuth2 :

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2,social
mvn spring-boot:run -Dspring-boot.run.profiles=dev,social
```

#### Solution 2 — Démarrer MySQL avec Docker (profil `mysql`)

```bash
# Démarre MySQL 8 + Adminer
docker compose up -d

# Vérifier que le conteneur est healthy
docker compose ps
docker logs prdv-mysql

# Lancer l'app en profil mysql
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

Adminer : http://localhost:8081 (serveur `mysql`, user `prdv` ou `root`, mdp `root`, base `prdv`)

Grâce à `spring-boot-docker-compose`, le conteneur MySQL démarre aussi automatiquement quand le profil `mysql` est actif, même sans `docker compose up -d` manuel, si Docker est disponible.

#### Solution 3 — Port 3306 déjà occupé

Si un MySQL natif tourne déjà sur votre machine :

```bash
# Vérifier ce qui écoute sur 3306
lsof -i :3306
# ou
netstat -tulpn | grep 3306

# Lancer le conteneur sur un autre port hôte
DB_PORT=13306 docker compose up -d

# Lancer l'app sur ce port
DB_PORT=13306 mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

Ou arrêter le MySQL natif :

```bash
sudo systemctl stop mysql
# ou
brew services stop mysql
```

#### Solution 4 — MySQL externe / distant

Configurer les variables d'environnement :

```bash
export DB_HOST=192.168.1.10
export DB_PORT=3306
export DB_NAME=prdv
export DB_USER=prdv
export DB_PASSWORD=secret
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

#### Vérifications

```bash
# Tester la connexion MySQL
mysql -h 127.0.0.1 -P 3306 -u root -p -e "SHOW DATABASES;"

# Logs du conteneur
docker logs prdv-mysql --tail 50
```

### Pourquoi le dialecte a été retiré / adapté ?

Ancienne config :

```yaml
hibernate:
  dialect: org.hibernate.dialect.MySQLDialect
```

Déclenchait `HHH90000025: MySQLDialect does not need to be specified explicitly` et cassait le profil H2. Désormais :

- Par défaut : `H2Dialect` (fichier H2)
- Profil `mysql` : `MySQLDialect` ou auto-détecté
- Profils `h2` / `dev` : `H2Dialect`

> Le warning `HHH90000025: H2Dialect does not need to be specified explicitly` qui peut
> rester dans les logs est **inoffensif** : il rappelle seulement que Hibernate sait
> détecter le dialecte tout seul via les métadonnées JDBC. Il ne bloque pas le démarrage.

### FailureAnalyzer et message d'aide

Un `DatabaseConnectionFailureAnalyzer` est ajouté (`src/main/java/com/prdv/rdv/common/web/DatabaseConnectionFailureAnalyzer.java`)
et enregistré via **`META-INF/spring.factories`** :

```properties
org.springframework.boot.diagnostics.FailureAnalyzer=\
com.prdv.rdv.common.web.DatabaseConnectionFailureAnalyzer
```

> ⚠️ Spring Boot 3 charge les `FailureAnalyzer` avec `SpringFactoriesLoader`, qui lit
> uniquement `META-INF/spring.factories`. Le format `META-INF/spring/<interface>`
> (utilisé pour `AutoConfiguration.imports`) **n'est pas lu** : l'analyseur enregistré
> à cet emplacement n'est jamais instancié et aucun message d'aide ne s'affiche.

En cas d'échec **MySQL**, Spring Boot affiche directement :

```
Impossible de se connecter a MySQL. La base de donnees est injoignable...
Solutions :
1) docker compose up -d
2) DB_PORT=13306 ...
3) mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

En cas d'échec **H2** (URL invalide comme `AUTO_SERVER=TRUE` + `DB_CLOSE_ON_EXIT=FALSE`,
ou fichier `./data/prdv.mv.db` verrouillé par un autre processus), l'analyseur affiche
les solutions H2 correspondantes au lieu des instructions MySQL — l'erreur H2 remontant
enveloppée dans un `GenericJDBCException` dont le message contient déjà
« unable to obtain isolated JDBC connection ».

De plus, `PrdvApplication` affiche un rappel sur `stderr`.

### Autres erreurs

#### `Failed to initialize JPA EntityManagerFactory`

Même cause racine que ci-dessus : la base n'est pas joignable. Suivre les solutions MySQL/H2.

#### `EncryptionKeyHolder` / `Cle de chiffrement non initialisee`

Vérifier que `CIPHER_SECRET` est défini ou que `prdv.security.cipher.secret` est présent dans `application.yml`. En dev, une valeur par défaut existe.

#### OTP non reçu

En développement sans SMTP, les OTP sont affichés dans les logs :

```
[NOTIFICATION EMAIL] Code OTP pour patient@... : 384921
```

Pour activer les vrais emails, définir `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`.

#### Problèmes Lombok / `cannot find symbol`

Assurez-vous d'utiliser JDK 17+ et que `lombok.version` est 1.18.48+ (déjà configuré dans `pom.xml`). Le `maven-compiler-plugin` doit avoir `<proc>full</proc>`.

```bash
java -version
mvn -version
```

#### Toujours bloqué ?

- `docker compose down -v && docker compose up -d` (reset volume)
- `mvn clean`
- Vérifiez Java 17+ : `java -version`
- Supprimez `./data/` si H2 fichier corrompu
- Ouvrez une issue avec le log complet et `docker compose ps`.

## `403 FORBIDDEN` — « Acces refuse : permission manquante » sur `/api/v1/health/**`

**Réponse typique :**

```json
{
  "timestamp": "2026-09-28T17:26:13.983Z",
  "status": 403,
  "code": "FORBIDDEN",
  "message": "Acces refuse : permission manquante",
  "path": "/api/v1/health/devices"
}
```

### Cause

Le jeton est valide (sinon la réponse serait `401`) : ce sont les **autorités
effectives** qui ne contiennent pas la permission exigée par le `@PreAuthorize`
du contrôleur (`profile.health.read` pour `GET /metrics`, `GET /series`,
`GET /alerts`, `GET /devices` ; `profile.health.write` pour les écritures).

Les autorités sont recalculées à **chaque requête** par
`AuthorizationQueryService` : permissions des rôles + permissions directes +
délégations actives. Une cause couvre la grande majorité des cas :

> **le rôle en base a été amorcé avant que la permission soit ajoutée au code.**

`DataSeeder` créait les rôles système « si absents » seulement : une permission
ajoutée par la suite (`profile.health.read` / `profile.health.write` du module
2.1) était inscrite au catalogue mais jamais propagée aux rôles déjà présents
dans `./data/prdv.mv.db` (ou dans MySQL). Le catalogue, le contrôleur et le
jeton sont donc tous corrects — seul le lien `roles_permissions` est incomplet.

Autre cause possible, cette fois voulue : le compte n'est pas un patient ni un
praticien (secrétaire, établissement, modération, support…) — ces rôles n'ont
aucune permission `profile.health.*`.

### Solutions

#### Solution 1 — Redémarrer l'application (recommandé)

Depuis la correction, `DataSeeder` **réconcilie** les rôles système avec la
définition du code à chaque démarrage (création si absent, sinon mise à jour du
jeu de permissions). Les autorités étant résolues à chaque requête, la
correction prend effet immédiatement :

```bash
# arrêter puis relancer
mvn spring-boot:run
```

Un ligne de log confirme la réconciliation :

```
WARN  c.p.r.iam.config.DataSeeder : Role systeme ROLE_PATIENT reconcilie avec le code :
     ajoute [profile.health.read, profile.health.write] / retire []
```

#### Solution 2 — Vérifier les rôles et leurs permissions

Avec un compte super-administrateur (`superadmin@prdv.app` / `Admin#2026!` par
défaut) :

```bash
curl -s localhost:8080/api/v1/admin/roles -H "Authorization: Bearer $TOKEN"
```

Chaque rôle doit exposer ses `permissionCodes` ; `ROLE_PATIENT` doit contenir
`profile.health.read` et `profile.health.write`, `ROLE_PRACTITIONER` au moins
`profile.health.read`.

#### Solution 3 — Rattacher un autre rôle au compte

Si le compte de test n'est pas un patient/praticien, un administrateur peut lui
attribuer le rôle voulu :

```bash
curl -X POST localhost:8080/api/v1/admin/users/1/roles \
  -H "Authorization: Bearer $adminToken" -H "Content-Type: application/json" \
  -d '{"roleNames":["ROLE_PATIENT"]}'
```

#### Solution 4 — Réamorcer la base (ancienne version du code)

Sans la réconciliation (build anterieur), il faut rejouer l'amorçage complet :

```bash
rm -rf ./data          # H2 fichier : supprime aussi les comptes créés
mvn spring-boot:run
```

### Tests de non-régression

- `src/test/java/com/prdv/rdv/iam/config/DataSeederRoleSyncTest.java` — création,
  réconciliation, rôle personnalisé jamais écrasé ;
- `src/test/java/com/prdv/rdv/iam/config/ControllerPermissionCoverageTest.java` —
  toute permission exigée par un `@PreAuthorize` existe au catalogue **et** est
  accordée à au moins un rôle système (un endpoint qui exige une permission que
  personne ne possède est détecté à la compilation des tests).

## `400 VALIDATION_ERROR` sur `POST /api/v1/practitioners/me/memberships`

**Réponse typique :**

```json
{
  "timestamp": "2026-10-03T10:00:00.000Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "...",
  "path": "/api/v1/practitioners/me/memberships"
}
```

### Causes possibles

1. **Confusion entre `users.id` et `establishment_profiles.id` (`Le compte cible n'est pas un etablissement`)** :
   - Au démarrage, `DataSeeder` crée le compte `superadmin@prdv.app` avec `users.id = 1` (`ProfileType.ADMIN`).
   - Les comptes suivants reçoivent `users.id = 2` (patient), `3` (praticien), `4` (établissement), tandis que la table `establishment_profiles` numérote son premier établissement à `id = 1`.
   - En envoyant `"establishmentUserId": 1` sur l'ancienne version, le service chargeait `users.id = 1` (`ADMIN`) au lieu du premier établissement.
   - **Corrigé** : `PractitionerRegistrationService` résout désormais `establishmentUserId` d'abord comme `users.id` de type `ESTABLISHMENT`, puis en repli comme `establishment_profiles.id`. Vous pouvez également lister les établissements existants via `GET /api/v1/establishments`.

2. **Valeur de `role` ou nom de champ non reconnu (`Requete invalide` / `Parametres invalides`)** :
   - Valeurs canoniques de `role` : `OWNER`, `EMPLOYEE`, `REPLACER`.
   - **Corrigé** :
     - `role` est désormais **optionnel** (défaut : `EMPLOYEE`) et accepte aussi le nom de champ `memberRole` ;
     - la désérialisation est insensible à la casse et aux accents et accepte les alias usuels (`TITULAIRE`, `GERANT`, `SALARIE`, `COLLABORATEUR`, `ASSOCIE`, `MEMBRE`, `PRATICIEN`, `MEDECIN`, `REMPLACANT`, etc.) ;
     - `establishmentUserId` accepte aussi l'alias `establishmentId` / `cabinetId` ;
     - `validFrom` et `validUntil` acceptent `YYYY-MM-DD`, un horodatage ISO-8601 (`2026-10-03T08:00:00Z`), le format français `DD/MM/YYYY`, ou `null` / `""`.

3. **Demande de rattachement déjà envoyée (`Un rattachement existe deja avec cet etablissement`)** :
   - **Corrigé** : si une demande `PENDING` existe déjà pour le couple `(praticien, établissement)`, un second `POST /api/v1/practitioners/me/memberships` met à jour la demande en attente de manière idempotente au lieu de renvoyer `400 VALIDATION_ERROR`.

4. **Dates incohérentes (`validUntil` antérieure à `validFrom`)** :
   - Assurez-vous que `validUntil >= validFrom` (ou laissez `validFrom` / `validUntil` à `null` pour un rattachement sans date de fin).

### Exemple de requête valide

```http
POST http://localhost:8080/api/v1/practitioners/me/memberships
Authorization: Bearer <practitionerAccessToken>
Content-Type: application/json

{
  "establishmentUserId": 1,
  "role": "EMPLOYEE",
  "validFrom": "2026-10-03",
  "validUntil": "2027-10-03"
}
```


## `500 INTERNAL_ERROR` — « Une erreur interne est survenue » sur `POST /api/v1/planning/agenda/slots`

**Réponse observée côté frontend :**

```json
{
  "timestamp": "2026-10-16T13:22:37.472Z",
  "status": 500,
  "code": "INTERNAL_ERROR",
  "message": "Une erreur interne est survenue",
  "path": "/api/v1/planning/agenda/slots"
}
```

### Cause racine du caractère « aveugle » de cette erreur

Le handler de dernier recours de `GlobalExceptionHandler` renvoyait ce corps **sans
journaliser l'exception** : la cause réelle (classe, message, stack trace) n'apparaissait
nulle part, ni côté serveur ni côté client. Toute panne d'infrastructure sur le chemin
d'écriture du planning se transformait donc en 500 muet.

Ce chemin est le suivant :

```
PlanningController.add
  → PlanningService.add            (1) contrôle du lieu d'exercice
  → JpaAgendaStore.update          (2) verrou global + lecture/écriture de l'agrégat
      → PlanningCoordinator.lock   (3) verrou pessimiste planning_write_mutex
      → decode(payload)            (4) JSON → Agenda (agrégat ET tous les autres agendas)
      → Agenda.addSlots            (5) règles métier du domaine
      → PlanningJournal.record     (6) audit + outbox, propagation MANDATORY
```

Les étapes (1) à (6) lèvent des `PlanningException` correctement traduites (400/403/404/409),
mais **toute autre exception** (persistance, verrou, sérialisation) tombait dans le 500 muet.

### Ce qui a changé

| Situation | Avant | Après |
|---|---|---|
| N'importe quelle exception non prévue | 500 `INTERNAL_ERROR`, **aucun log** | 500 `INTERNAL_ERROR` + `ERROR` avec méthode, URI, classe, message et stack trace |
| Verrou pessimiste perdu / attente trop longue (`ConcurrencyFailureException`, `QueryTimeoutException`) | 500 muet | 409 `CONCURRENT_UPDATE` (« merci de rejouer la requête ») |
| Base injoignable (`TransientDataAccessException`, `NonTransientDataAccessResourceException` dont hérite `CannotGetJdbcConnectionException`) | 500 muet | 503 `DATABASE_UNAVAILABLE` |
| Contrainte SQL violée (`DataIntegrityViolationException`) | 500 muet | 409 `DATA_CONFLICT` |
| Invariant applicatif cassé (`IllegalStateException`, ex. agenda illisible) | 500 `INTERNAL_ERROR` sans détail | 500 `APPLICATION_STATE_ERROR` avec le message (dont l'identifiant d'agenda) |
| Un **autre** agenda illisible en base | faisait échouer *toutes* les écritures du planning | ignoré + `ERROR` journalisé (le contrôle anti double réservation inter-praticiens continue sur les agendas lisibles) |
| Rendez-vous orphelin (créneau supprimé) | `PLANNING_NOT_FOUND` « Creneau introuvable » trompeur, déclenché par le contrôle inter-praticiens sur une écriture sans rapport | ignoré pour la comparaison de plages |
| Ligne `planning_write_mutex` absente | `IllegalStateException: Verrou planning absent` → 500 définitif sur toutes les écritures | recréée à la volée (`WARN`), puis verrouillage normal |
| `site` absent ou vide dans le corps de requête | `InvalidDataAccessApiUsageException` (`findById(null)`) → 500 | 400 `PLANNING_INVALID` « Lieu d'exercice inconnu » |

Les refus métier (400/403/404/409) sont désormais aussi tracés, au niveau `DEBUG`
(`logging.level.com.prdv.rdv=DEBUG`, ou profil `dev`).

### Étape 1 — lire le log serveur

Relancer la requête puis chercher la ligne émise par le handler :

```
ERROR c.p.r.common.web.GlobalExceptionHandler : POST /api/v1/planning/agenda/slots -> <classe> : <message>
<stack trace complète>
```

La classe affichée donne directement la famille de panne :

| Classe dans le log | Signification | Action |
|---|---|---|
| `IllegalStateException: Lecture de l'agenda N impossible` | payload JSON corrompu ou écrit par un ancien schéma dans `planning_agendas` | voir « Réparer un agenda illisible » ci-dessous |
| `IllegalStateException: Verrou planning demandé hors d'une transaction active` | un appel à `AgendaStore` ne passe pas par le proxy `@Transactional` | vérifier que la mutation passe par `JpaAgendaStore.update` / `create` |
| `PessimisticLockingFailureException`, `CannotAcquireLockException`, `DeadlockLoserDataAccessException`, `QueryTimeoutException` | contention sur le verrou d'écriture global (plusieurs `POST` parallèles du frontend, ou worker `WaitingOfferWorker`) | désormais 409 rejouable : sérialiser les appels côté frontend ; en dev, `prdv.planning.waiting-offers.enabled=false` |
| `CannotGetJdbcConnectionException`, `DataAccessResourceFailureException` | base injoignable / pool Hikari épuisé | voir la section « Communications link failure » |
| `DataIntegrityViolationException` | contrainte SQL (ex. colonne `nullable=false` alimentée à `NULL`) | lire la contrainte nommée dans la stack trace |
| `TransactionRequiredException` | `PlanningJournal.record` (propagation `MANDATORY`) appelé hors transaction | même cause que « hors d'une transaction active » |
| `NullPointerException` | anomalie de code : la stack trace pointe la ligne exacte | à corriger (le 500 reste renvoyé, mais tracé) |

### Étape 2 — vérifier les trois prérequis données

Console H2 (`http://localhost:8080/h2-console`) ou Adminer (`http://localhost:8081`) :

```sql
-- (a) Le verrou global doit contenir exactement une ligne (id = 1)
SELECT * FROM planning_write_mutex;

-- (b) Tous les payloads doivent être du JSON décodable (une ligne illisible
--     était auparavant fatale à toutes les écritures ; elle est maintenant ignorée + journalisée)
SELECT practitioner_id, version, LENGTH(payload) AS octets, LEFT(payload, 60) AS debut
FROM planning_agendas;

-- (c) Le "site" envoyé par le frontend doit être un ID existant ET rattaché au praticien du JWT
SELECT id, practitioner_user_id, name, city FROM profile_practice_locations;
```

**Réparer un agenda illisible** (dev uniquement — l'agrégat est perdu) :

```sql
DELETE FROM planning_agendas WHERE practitioner_id = <id>;
```

puis rouvrir l'agenda : `POST /api/v1/planning/agenda`.

### Étape 3 — vérifier le contrat de la requête

Ces erreurs renvoient 400/403/404 (pas 500), mais elles sont les plus fréquentes côté frontend :

```http
POST http://localhost:8080/api/v1/planning/agenda/slots
Authorization: Bearer <practitionerAccessToken>
Content-Type: application/json

[
  {
    "id": "6f1c2b90-6d0e-4a5b-9c3d-2b8f0a1e7d55",
    "site": "<ID du lieu d'exercice, pas son libellé>",
    "start": "2026-10-20T09:00:00Z",
    "end": "2026-10-20T09:30:00Z",
    "type": "NORMAL",
    "audience": "ALL"
  }
]
```

| Exigence | Erreur si absente |
|---|---|
| Le corps est un **tableau** (1 à 500 créneaux), pas un objet `{ "slots": [...] }` | 400 `VALIDATION_ERROR` (« Requete invalide : JSON parse error ») |
| `id` : UUID **fourni par le client**, non présent dans l'agenda | 400 si absent ; 409 `PLANNING_CONFLICT` « Identifiant dupliqué » si déjà connu |
| `site` : identifiant d'un `PracticeLocation` **appartenant au praticien authentifié** (créé via `/api/v1/locations`) | 400 `PLANNING_INVALID` « Lieu d'exercice inconnu » ; 403 `PLANNING_FORBIDDEN` « Ce lieu n'appartient pas au praticien » |
| `start` / `end` : instants ISO-8601 **avec fuseau** (`...Z` ou `+03:00`). Une date/heure locale nue (`2026-10-20T09:00:00`) est refusée | 400 `VALIDATION_ERROR` |
| `start` strictement dans le futur, `start < end` | 400 `PLANNING_INVALID` « Creneau passe » / « Creneau invalide » |
| `type` ∈ `NORMAL, FIRST_VISIT, FOLLOW_UP, EMERGENCY, TELECONSULTATION, HOME_VISIT, TECHNICAL_ACT, MEETING, ADMINISTRATIVE, ABSENCE` ; `audience` ∈ `ALL, NEW_PATIENTS, KNOWN_PATIENTS` (majuscules) | 400 `VALIDATION_ERROR` |
| Aucun chevauchement avec les créneaux existants (marge de trajet inter-sites comprise) | 409 `PLANNING_CONFLICT` « Chevauchement ou trajet insuffisant » |
| Rôle `PRACTITIONER` sur le JWT | 403 `FORBIDDEN` |
| Agenda non initialisé : `GET /agenda/rules`, `GET /agenda/slots` et `GET /agenda/settings` initialisent automatiquement un agenda vide pour le praticien authentifié. `POST /api/v1/planning/agenda` reste disponible et est idempotent (201 à la création, 200 si l'agenda existe déjà). Une mutation envoyée avant toute initialisation reçoit encore 404 `PLANNING_NOT_FOUND` « Agenda introuvable ». |

Un `POST` dupliqué par le frontend (double invocation d'effet React en `StrictMode`,
retry automatique) n'aboutit pas à un 500 : le second appel échoue en 409
« Identifiant dupliqué » puisque l'UUID est généré côté client.

### Tests de non-régression

- `common/web/GlobalExceptionHandlerTest` — journalisation et statuts (409/503/500 détaillé),
  absence de mapping `@ExceptionHandler` ambigu, choix du handler le plus spécifique ;
- `planning/adapter/out/persistence/JpaAgendaStoreTest#unreadableNeighbouringAgendaDoesNotBlockWrites`
  — un agenda tiers illisible ne bloque plus les écritures ;
- `planning/adapter/out/profile/ProfilePracticeSiteAccessTest#rejectsMissingSiteWithoutQueryingTheRepository`
  — `site` nul/vide → `PlanningException(INVALID)` sans jamais appeler `findById(null)` ;
- `planning/e2e/PlanningApiE2ETest#slotCreationReportsFrontendMistakesWithExplicitStatuses`
  — contrat HTTP complet de `POST /agenda/slots` (404, 400, 403, 400, 200).
