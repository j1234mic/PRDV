# Dépannage — PRDV

## `Communications link failure` / `Connexion refusée` au démarrage

Stack trace typique :

```
WARN  o.h.engine.jdbc.spi.SqlExceptionHelper : SQL Error: 0, SQLState: 08S01
ERROR o.h.engine.jdbc.spi.SqlExceptionHelper : Communications link failure
Caused by: java.net.ConnectException: Connexion refusée
...
Failed to initialize JPA EntityManagerFactory: Unable to build Hibernate SessionFactory
...
Error creating bean with name 'securityConfig' ... Cannot resolve reference to bean 'entityManagerFactory'
```

### Cause

L'application tente de se connecter à MySQL (`jdbc:mysql://localhost:3306/prdv`) mais :

- MySQL n'est pas démarré, ou
- Le port 3306 est occupé par un autre MySQL natif, ou
- Les variables `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD` sont incorrectes.

### Solutions

#### 1. Démarrer MySQL via Docker Compose (recommandé)

```bash
docker compose up -d
docker compose ps        # doit afficher prdv-mysql healthy
docker compose logs mysql
mvn spring-boot:run
```

Adminer (UI web MySQL) : http://localhost:8081

#### 2. Port 3306 déjà occupé

Si un MySQL est déjà installé sur la machine :

```bash
# Vérifier ce qui écoute sur 3306
lsof -i :3306
# ou
netstat -tulpn | grep 3306

# Lancer le conteneur sur un autre port hôte
DB_PORT=13306 docker compose up -d
DB_PORT=13306 mvn spring-boot:run
```

#### 3. Sans Docker — Profil H2 en mémoire

Depuis la version avec support H2, vous pouvez démarrer sans MySQL :

```bash
# H2 minimal
mvn spring-boot:run -Dspring-boot.run.profiles=h2

# H2 + logs SQL + console web
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- Console H2 : http://localhost:8080/h2-console
  - JDBC URL : `jdbc:h2:mem:prdv`
  - User : `sa`
  - Password : (vide)
- Swagger UI : http://localhost:8080/swagger-ui.html

Combinaison avec OAuth2 :

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2,social
```

#### 4. Vérifier la configuration

`src/main/resources/application.yml` :

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:prdv}?...
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:root}
```

Variables d'environnement supportées :

| Variable | Défaut | Rôle |
|---|---|---|
| `DB_HOST` | `localhost` | Hôte MySQL |
| `DB_PORT` | `3306` | Port MySQL |
| `DB_NAME` | `prdv` | Base |
| `DB_USER` | `root` | User |
| `DB_PASSWORD` | `root` | Password |

#### 5. Logs utiles

```bash
# Relancer avec debug
mvn spring-boot:run -Dspring-boot.run.arguments=--debug

# Vérifier que le FailureAnalyzer custom s'affiche
# Il doit proposer 3 solutions (docker, port alternatif, h2)
```

### Pourquoi le dialecte a été retiré ?

Ancienne config :

```yaml
hibernate:
  dialect: org.hibernate.dialect.MySQLDialect
```

Déclenchait `HHH90000025: MySQLDialect does not need to be specified explicitly`
et cassait le profil H2. Le dialecte est désormais auto-détecté.

### Toujours bloqué ?

- `docker compose down -v && docker compose up -d` (reset volume)
- `mvn clean`
- Vérifiez Java 17+ : `java -version`
- Ouvrez une issue avec le log complet et `docker compose ps`.
