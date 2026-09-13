# Dépannage — Erreurs courantes

## `Communications link failure` / `Connexion refusée` / `Unable to open JDBC Connection`

**Cause :** L'application tente de se connecter à MySQL (`localhost:3306`) mais aucune instance MySQL n'est joignable.

```
SQL Error: 0, SQLState: 08S01
Communications link failure
The last packet sent successfully to the server was 0 milliseconds ago.
Caused by: java.net.ConnectException: Connexion refusée
...
Unable to build Hibernate SessionFactory
```

### Solution 1 — Utiliser H2 embarquée (recommandé pour dev rapide)

Depuis la version avec support H2, `mvn spring-boot:run` utilise H2 par défaut, sans Docker.

```bash
mvn spring-boot:run
# ou explicitement
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

- Fichier créé : `./data/prdv.mv.db`
- Console H2 : http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/prdv`, user `sa`)

Si vous aviez déjà un `application.yml` qui forçait MySQL, vérifiez que vous êtes bien sur la dernière version ou lancez avec le profil `h2`.

### Solution 2 — Démarrer MySQL avec Docker

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

### Solution 3 — Port 3306 déjà occupé

Si un MySQL natif tourne déjà sur votre machine :

```bash
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

### Solution 4 — MySQL externe / distant

Configurer les variables d'environnement :

```bash
export DB_HOST=192.168.1.10
export DB_PORT=3306
export DB_NAME=prdv
export DB_USER=prdv
export DB_PASSWORD=secret
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

### Vérifications

```bash
# Tester la connexion MySQL
mysql -h 127.0.0.1 -P 3306 -u root -p -e "SHOW DATABASES;"

# Vérifier qu'aucun autre process n'écoute sur 3306
lsof -i :3306
# ou
netstat -tulpn | grep 3306

# Logs du conteneur
docker logs prdv-mysql --tail 50
```

## `Failed to initialize JPA EntityManagerFactory`

Même cause racine que ci-dessus : la base n'est pas joignable. Suivre les solutions MySQL/H2.

## `EncryptionKeyHolder` / `Cle de chiffrement non initialisee`

Vérifier que `CIPHER_SECRET` est défini ou que `prdv.security.cipher.secret` est présent dans `application.yml`. En dev, une valeur par défaut existe.

## OTP non reçu

En développement sans SMTP, les OTP sont affichés dans les logs :

```
[NOTIFICATION EMAIL] Code OTP pour patient@... : 384921
```

Pour activer les vrais emails, définir `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`.

## Problèmes Lombok / `cannot find symbol`

Assurez-vous d'utiliser JDK 17+ et que `lombok.version` est 1.18.48+ (déjà configuré dans `pom.xml`). Le `maven-compiler-plugin` doit avoir `<proc>full</proc>`.

```bash
java -version
mvn -version
```
