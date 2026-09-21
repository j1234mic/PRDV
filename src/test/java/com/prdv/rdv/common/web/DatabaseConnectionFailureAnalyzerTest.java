package com.prdv.rdv.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifie que l'analyseur d'echec oriente l'utilisateur vers la bonne cause :
 * une erreur H2 (URL invalide, fichier verrouille) ne doit pas etre annoncee
 * comme un MySQL injoignable, et inversement.
 */
class DatabaseConnectionFailureAnalyzerTest {

    private final DatabaseConnectionFailureAnalyzer analyzer = new DatabaseConnectionFailureAnalyzer();

    @Test
    void explains_h2_auto_server_conflict_instead_of_blaming_mysql() {
        // Reproduction fidele de la chaine remontee par Hibernate/Hikari :
        // le message H2 est deja present dans les causes intermediaires.
        Throwable h2 = new RuntimeException("Fonctionnalité non supportée: "
                + "\"AUTO_SERVER=TRUE && DB_CLOSE_ON_EXIT=FALSE\"\n"
                + "Feature not supported: \"AUTO_SERVER=TRUE && DB_CLOSE_ON_EXIT=FALSE\" [50100-224]");
        Throwable jdbc = new RuntimeException("Unable to open JDBC Connection for DDL execution ["
                + h2.getMessage() + "] [n/a]", h2);
        Throwable jpa = new RuntimeException("Failed to initialize JPA EntityManagerFactory: "
                + "Unable to build Hibernate SessionFactory; nested exception is "
                + "org.hibernate.exception.GenericJDBCException: " + jdbc.getMessage(), jdbc);
        Throwable web = new RuntimeException("Unable to start web server", jpa);

        FailureAnalysis analysis = analyzer.analyze(web);

        assertNotNull(analysis, "l'analyseur aurait du diagnostiquer l'erreur H2");
        assertTrue(analysis.getDescription().contains("Configuration H2 invalide"),
                analysis.getDescription());
        assertTrue(analysis.getDescription().contains("AUTO_SERVER=TRUE"),
                analysis.getDescription());
        assertTrue(analysis.getAction().contains("DB_CLOSE_ON_EXIT=FALSE"), analysis.getAction());
        assertFalse(analysis.getAction().contains("docker compose up -d"),
                "les instructions MySQL ne doivent pas masquer l'erreur H2 : " + analysis.getAction());
    }

    @Test
    void explains_locked_h2_file() {
        Throwable h2 = new RuntimeException("Database may be already in use: "
                + "Locked by file: null, session: 10 [90020-224]");

        FailureAnalysis analysis = analyzer.analyze(new RuntimeException("Unable to start web server", h2));

        assertNotNull(analysis);
        assertTrue(analysis.getDescription().contains("verrouillee"), analysis.getDescription());
        assertTrue(analysis.getAction().contains("lock.db"), analysis.getAction());
    }

    @Test
    void still_explains_unreachable_mysql() {
        Throwable mysql = new RuntimeException(
                "Communications link failure\nConnexion refusée (Connection refused)");
        Throwable jdbc = new RuntimeException("Unable to obtain isolated JDBC connection ["
                + mysql.getMessage() + "]", mysql);

        FailureAnalysis analysis = analyzer.analyze(new RuntimeException("Unable to start web server", jdbc));

        assertNotNull(analysis);
        assertTrue(analysis.getDescription().contains("MySQL"), analysis.getDescription());
        assertTrue(analysis.getAction().contains("docker compose up -d"), analysis.getAction());
    }

    @Test
    void stays_silent_on_unrelated_failure() {
        assertNull(analyzer.analyze(new IllegalStateException("boom")));
    }
}
