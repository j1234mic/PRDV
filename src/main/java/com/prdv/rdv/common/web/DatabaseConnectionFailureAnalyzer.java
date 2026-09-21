package com.prdv.rdv.common.web;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Analyse les echecs de connexion JDBC et propose une solution actionable.
 *
 * <p>Deux familles d'erreurs sont distinguees :
 * <ul>
 *     <li><b>H2</b> : URL invalide (parametres incompatibles entre eux, par ex.
 *     {@code AUTO_SERVER=TRUE} + {@code DB_CLOSE_ON_EXIT=FALSE}) ou fichier verrouille
 *     par un autre processus ;</li>
 *     <li><b>MySQL</b> : base injoignable (non demarree, port occupe, mauvais
 *     identifiants).</li>
 * </ul>
 *
 * <p>Aucune dependance directe vers les drivers (scope runtime) n'est importee :
 * la detection se fait par nom de classe et par message, pour ne pas casser la
 * compilation quel que soit le profil actif.
 */
public class DatabaseConnectionFailureAnalyzer
        extends AbstractFailureAnalyzer<Throwable> {

    /** Profondeur maximale de la chaine de causes inspectee. */
    private static final int MAX_DEPTH = 25;

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, Throwable cause) {
        List<Throwable> chain = causes(cause != null ? cause : rootFailure);

        // 1) H2 d'abord : une erreur H2 remonte souvent enveloppee dans un
        //    GenericJDBCException dont le message contient deja « unable to obtain
        //    isolated JDBC connection », qui serait sinon pris pour une erreur MySQL.
        for (Throwable t : chain) {
            if (isUnsupportedH2Setting(t)) {
                return unsupportedH2SettingAnalysis(t);
            }
        }
        for (Throwable t : chain) {
            if (isH2FileLockProblem(t)) {
                return h2FileLockAnalysis(t);
            }
        }

        // 2) Sinon : base (MySQL) injoignable.
        for (Throwable t : chain) {
            if (isConnectionFailure(t)) {
                return unreachableDatabaseAnalysis(t);
            }
        }
        return null;
    }

    /**
     * Retourne la chaine de causes (bornee) a partir de la throwable donnee.
     */
    private List<Throwable> causes(Throwable start) {
        List<Throwable> chain = new ArrayList<>();
        Throwable cursor = start;
        while (cursor != null && chain.size() < MAX_DEPTH && !chain.contains(cursor)) {
            chain.add(cursor);
            cursor = cursor.getCause();
        }
        return chain;
    }

    // ------------------------------------------------------------------ H2

    /**
     * H2 refuse certaines combinaisons de parametres d'URL, par exemple :
     * {@code Feature not supported: "AUTO_SERVER=TRUE && DB_CLOSE_ON_EXIT=FALSE"}
     * (SQLState HYC00, code 50100).
     */
    private boolean isUnsupportedH2Setting(Throwable t) {
        String className = t.getClass().getName();
        String msg = lower(t.getMessage());
        boolean fromH2 = className.startsWith("org.h2.") || looksLikeH2Message(msg);
        boolean unsupported = className.contains("FeatureNotSupported")
                || msg.contains("feature not supported")
                || msg.contains("fonctionnalite non supportee")
                || msg.contains("unsupported connection setting");
        return fromH2 && unsupported;
    }

    /**
     * Le message H2 est souvent recopie tel quel par Hibernate/Hikari dans une
     * exception non-H2 : on le reconnait alors a ses parametres d'URL ou a son code
     * d'erreur (50100 = « feature not supported »).
     */
    private boolean looksLikeH2Message(String msg) {
        return msg.contains("jdbc:h2")
                || msg.contains("auto_server")
                || msg.contains("db_close_on_exit")
                || msg.contains("db_close_delay")
                || msg.contains("[50100");
    }

    private FailureAnalysis unsupportedH2SettingAnalysis(Throwable cause) {
        String feature = extractQuotedFeature(cause.getMessage());
        String description = "Configuration H2 invalide : le pilote refuse "
                + (feature.isEmpty() ? "un parametre de l'URL JDBC" : "la combinaison " + feature)
                + " (erreur 50100, SQLState HYC00). La SessionFactory Hibernate ne peut donc "
                + "pas etre construite et le contexte Spring demarre en echec.";
        String action = """
                Solutions :
                1) Utiliser l'URL H2 fichier par defaut (un seul processus, sans AUTO_SERVER) :
                   jdbc:h2:file:./data/prdv;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
                   -> c'est la valeur corrigee dans src/main/resources/application.yml (git pull)

                2) Besoin d'ouvrir ./data/prdv.mv.db avec un outil externe (DBeaver, IntelliJ)
                   pendant que l'application tourne ? Activer le mixed mode SANS
                   DB_CLOSE_ON_EXIT=FALSE (H2 exige son shutdown hook dans ce mode) :
                   SPRING_DATASOURCE_URL='jdbc:h2:file:./data/prdv;MODE=MySQL;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE' mvn spring-boot:run

                3) Base en memoire (aucun fichier, aucun conflit possible) :
                   mvn spring-boot:run -Dspring-boot.run.profiles=h2

                Verifiez aussi qu'aucune surcharge locale ne reintroduit des parametres
                incompatibles : variable d'environnement SPRING_DATASOURCE_URL,
                -Dspring-boot.run.arguments=--spring.datasource.url=..., application-<profil>.yml.
                Details : docs/TROUBLESHOOTING.md
                """;
        return new FailureAnalysis(description, action, cause);
    }

    /**
     * Extrait la portion entre guillemets du message H2, ex.
     * {@code Fonctionnalite non supportee: "AUTO_SERVER=TRUE && DB_CLOSE_ON_EXIT=FALSE"}.
     */
    private String extractQuotedFeature(String message) {
        if (message == null) {
            return "";
        }
        int start = message.indexOf('"');
        int end = message.indexOf('"', start + 1);
        return (start >= 0 && end > start) ? message.substring(start, end + 1) : "";
    }

    /**
     * Base H2 fichier deja ouverte par un autre JVM, ou verrou residuel
     * ({@code Database may be already in use ... Locked by file}).
     */
    private boolean isH2FileLockProblem(Throwable t) {
        String msg = lower(t.getMessage());
        return msg.contains("database may be already in use")
                || msg.contains("locked by file")
                || (msg.contains("lock file") && msg.contains("h2"));
    }

    private FailureAnalysis h2FileLockAnalysis(Throwable cause) {
        String description = "La base H2 fichier ./data/prdv.mv.db est verrouillee : un autre "
                + "processus l'utilise deja, ou un arret precedent a laisse un fichier .lock.db.";
        String action = """
                Solutions :
                1) Fermer l'autre instance de l'application :
                   ps aux | grep -i prdv
                   lsof ./data/prdv.mv.db

                2) Supprimer un verrou residuel (application arretee) :
                   rm -f ./data/*.lock.db

                3) Partager le fichier avec un outil externe : mixed mode SANS
                   DB_CLOSE_ON_EXIT=FALSE :
                   SPRING_DATASOURCE_URL='jdbc:h2:file:./data/prdv;MODE=MySQL;AUTO_SERVER=TRUE' mvn spring-boot:run

                4) Ou passer en base memoire :
                   mvn spring-boot:run -Dspring-boot.run.profiles=h2

                Details : docs/TROUBLESHOOTING.md
                """;
        return new FailureAnalysis(description, action, cause);
    }

    // -------------------------------------------------------------- MySQL

    private FailureAnalysis unreachableDatabaseAnalysis(Throwable cause) {
        String description = "Impossible de se connecter a MySQL. "
                + "La base de donnees est injoignable (Communications link failure / Connexion refusee).";
        String action = """
                Solutions :
                1) Demarrez MySQL avec Docker Compose :
                   docker compose up -d
                   docker compose ps
                   mvn spring-boot:run -Dspring-boot.run.profiles=mysql

                2) Si le port 3306 est occupe par un MySQL natif :
                   DB_PORT=13306 docker compose up -d
                   DB_PORT=13306 mvn spring-boot:run -Dspring-boot.run.profiles=mysql

                3) Sans Docker (developpement rapide) :
                   mvn spring-boot:run
                   -> H2 fichier ./data/prdv.mv.db, console : http://localhost:8080/h2-console
                   mvn spring-boot:run -Dspring-boot.run.profiles=h2
                   -> H2 memoire, console : http://localhost:8080/h2-console (JDBC URL: jdbc:h2:mem:prdv, user: sa)

                Verifiez aussi les variables DB_HOST, DB_PORT, DB_USER, DB_PASSWORD.
                Voir docs/TROUBLESHOOTING.md pour plus de details.
                """;
        return new FailureAnalysis(description, action, cause);
    }

    private boolean isConnectionFailure(Throwable t) {
        String className = t.getClass().getName();
        if (className.contains("JDBCConnectionException")
                || className.contains("CommunicationsException")
                || className.contains("CJCommunicationsException")
                || className.contains("HikariPool$PoolInitializationException")) {
            return true;
        }
        String msg = lower(t.getMessage());
        return msg.contains("communications link failure")
                || msg.contains("connexion refusee")
                || msg.contains("connection refused")
                || msg.contains("could not obtain connection")
                || msg.contains("unable to open jdbc connection")
                || msg.contains("unable to obtain isolated jdbc connection");
    }

    /**
     * Message normalise (minuscules, sans accents) pour une detection insensible
     * a la locale du pilote (le driver H2 renvoie des messages francais ou anglais
     * selon la JVM).
     */
    private static String lower(String message) {
        if (message == null) {
            return "";
        }
        String normalized = Normalizer.normalize(message, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT);
    }
}
