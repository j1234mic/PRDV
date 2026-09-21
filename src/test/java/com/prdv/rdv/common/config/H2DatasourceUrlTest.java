package com.prdv.rdv.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Garde-fou de regression sur l'URL H2 du profil par defaut.
 *
 * <p>H2 2.x refuse la combinaison {@code AUTO_SERVER=TRUE} + {@code DB_CLOSE_ON_EXIT=FALSE}
 * et leve {@code Feature not supported: "AUTO_SERVER=TRUE && DB_CLOSE_ON_EXIT=FALSE"}
 * (code 50100, SQLState HYC00). L'application echoue alors au demarrage
 * (« Unable to build Hibernate SessionFactory ») sans message evident.
 *
 * <p>Le second test ouvre reellement une base <b>fichier</b> : une base
 * {@code mem:} ignore AUTO_SERVER et ne reproduirait pas le bug.
 */
class H2DatasourceUrlTest {

    private static final Path MAIN_CONFIG = Path.of("src", "main", "resources", "application.yml");

    /** Repertoire temporaire par test (nouvelle instance = nouveau repertoire). */
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("application.yml ne melange pas AUTO_SERVER=TRUE et DB_CLOSE_ON_EXIT=FALSE")
    void default_url_does_not_mix_auto_server_with_db_close_on_exit_false() throws IOException {
        String url = defaultDatasourceUrl();
        boolean autoServer = containsSetting(url, "auto_server=true");
        boolean closeOnExitDisabled = containsSetting(url, "db_close_on_exit=false");

        assertFalse(autoServer && closeOnExitDisabled,
                "H2 2.x rejette AUTO_SERVER=TRUE avec DB_CLOSE_ON_EXIT=FALSE (erreur 50100) : "
                        + "retirez l'un des deux parametres dans " + MAIN_CONFIG + " -> " + url);
    }

    @Test
    @DisplayName("l'URL H2 fichier configuree est acceptee par le pilote")
    void default_url_opens_a_file_database() throws Exception {
        String url = relocateTo(defaultDatasourceUrl(), tempDir.resolve("prdv"));

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            String product = connection.getMetaData().getDatabaseProductName();
            assertNotNull(product);
            assertTrue(product.toLowerCase(Locale.ROOT).contains("h2"),
                    "le pilote aurait du ouvrir une base H2 : " + product);
        } finally {
            shutdown(url);
        }
    }

    /**
     * {@code DB_CLOSE_DELAY=-1} garde la base ouverte (et le fichier verrouille)
     * jusqu'a l'extinction du JVM : on demande la fermeture pour que le repertoire
     * temporaire du test puisse etre nettoye.
     */
    private static void shutdown(String url) {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        } catch (Exception ignored) {
            // Nettoyage best-effort : le JVM de test ferme la base en sortant.
        }
    }

    @SuppressWarnings("unchecked")
    private String defaultDatasourceUrl() throws IOException {
        assertTrue(Files.exists(MAIN_CONFIG),
                "application.yml introuvable : lancez les tests depuis la racine du projet");
        try (InputStream in = Files.newInputStream(MAIN_CONFIG)) {
            Map<String, Object> root = new Yaml().load(in);
            Map<String, Object> spring = (Map<String, Object>) root.get("spring");
            Map<String, Object> datasource = (Map<String, Object>) spring.get("datasource");
            Object url = datasource.get("url");
            assertNotNull(url, "spring.datasource.url absent de " + MAIN_CONFIG);
            return url.toString();
        }
    }

    private static boolean containsSetting(String url, String setting) {
        return url.toLowerCase(Locale.ROOT).replace(" ", "").contains(setting);
    }

    private static String relocateTo(String url, Path databaseFile) {
        String relocated = url.replace("./data/prdv", databaseFile.toString());
        assertFalse(relocated.equals(url) && url.contains("jdbc:h2:file:"),
                "Impossible de relocaliser la base H2 fichier dans le repertoire temporaire : " + url);
        return relocated;
    }
}
