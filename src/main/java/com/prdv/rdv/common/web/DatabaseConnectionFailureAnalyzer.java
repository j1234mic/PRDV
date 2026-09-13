package com.prdv.rdv.common.web;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Analyse les echecs de connexion JDBC (MySQL non demarre) et propose une
 * solution actionable a l'utilisateur.
 *
 * <p>Aucune dependance directe vers le driver MySQL (scope runtime) n'est
 * importee : la detection se fait par nom de classe et message, pour ne pas
 * casser la compilation en profil H2.
 */
public class DatabaseConnectionFailureAnalyzer
        extends AbstractFailureAnalyzer<Throwable> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, Throwable cause) {
        Throwable current = cause != null ? cause : rootFailure;
        Throwable cursor = current;
        int depth = 0;
        while (cursor != null && depth < 25) {
            if (isConnectionFailure(cursor)) {
                String description = "Impossible de se connecter a MySQL. "
                        + "La base de donnees est injoignable (Communications link failure / Connexion refusee).";
                String action = """
                        Solutions :
                        1) Demarrez MySQL avec Docker Compose :
                           docker compose up -d
                           docker compose ps
                           mvn spring-boot:run

                        2) Si le port 3306 est occupe par un MySQL natif :
                           DB_PORT=13306 docker compose up -d
                           DB_PORT=13306 mvn spring-boot:run

                        3) Sans Docker (developpement rapide, base en memoire) :
                           mvn spring-boot:run -Dspring-boot.run.profiles=h2
                           ou
                           mvn spring-boot:run -Dspring-boot.run.profiles=dev
                           -> Console H2 : http://localhost:8080/h2-console (JDBC URL: jdbc:h2:mem:prdv, user: sa)

                        Verifiez aussi les variables DB_HOST, DB_PORT, DB_USER, DB_PASSWORD.
                        Voir docs/TROUBLESHOOTING.md pour plus de details.
                        """;
                return new FailureAnalysis(description, action, cause);
            }
            cursor = cursor.getCause();
            depth++;
        }
        return null;
    }

    private boolean isConnectionFailure(Throwable t) {
        String className = t.getClass().getName();
        if (className.contains("JDBCConnectionException")
                || className.contains("CommunicationsException")
                || className.contains("CJCommunicationsException")
                || className.contains("HikariPool$PoolInitializationException")) {
            return true;
        }
        String msg = t.getMessage();
        if (msg != null) {
            String lower = msg.toLowerCase();
            if (lower.contains("communications link failure")
                    || lower.contains("connexion refusée")
                    || lower.contains("connexion refusee")
                    || lower.contains("connection refused")
                    || lower.contains("could not obtain connection")
                    || lower.contains("unable to open jdbc connection")
                    || lower.contains("unable to obtain isolated jdbc connection")) {
                return true;
            }
        }
        return false;
    }
}
