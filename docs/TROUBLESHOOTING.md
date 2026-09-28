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
