package com.prdv.rdv.common.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.prdv.rdv.common.persistence.EnumColumnMigration.Dialect;
import com.prdv.rdv.common.persistence.EnumColumnMigration.EnumColumn;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;

/**
 * Non-régression de l'erreur H2 {@code 22030} « Valeur non permise pour la colonne ».
 *
 * <p>Une base créée avant l'ajout des actions du module 2 gardait {@code audit_logs.action} en
 * {@code ENUM} à 25 valeurs ; l'insertion de {@code PROFILE_UPDATED} échouait.
 */
class EnumColumnMigrationTest {

    /** Les 25 actions du module 1, telles que figées dans la base du journal d'erreur. */
    private static final List<String> LEGACY_AUDIT_ACTIONS = List.of(
            "USER_REGISTERED", "OTP_REQUESTED", "OTP_VERIFIED", "LOGIN_SUCCESS", "LOGIN_FAILED",
            "LOGIN_SUSPICIOUS", "TOKEN_REFRESHED", "LOGOUT", "MFA_ENABLED", "MFA_DISABLED",
            "PASSWORD_CHANGED", "ACCOUNT_LOCKED", "SESSION_REVOKED", "ROLE_ASSIGNED", "ROLE_CREATED",
            "DELEGATION_GRANTED", "DELEGATION_REVOKED", "KYC_DOCUMENT_UPLOADED", "KYC_DOCUMENT_REVIEWED",
            "PRACTITIONER_VALIDATED", "ESTABLISHMENT_VALIDATED", "ACCOUNT_SUSPENDED", "ACCOUNT_ACTIVATED",
            "ACCOUNT_ANONYMIZED", "SOCIAL_LOGIN");

    private Connection connection;

    @BeforeEach
    void openDatabase() throws SQLException {
        // Base mémoire propre à chaque test, en mode MySQL comme la base de l'application
        connection = DriverManager.getConnection(
                "jdbc:h2:mem:enum-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
    }

    @AfterEach
    void closeDatabase() throws SQLException {
        execute("SHUTDOWN");
        connection.close();
    }

    @Test
    @DisplayName("après conversion, PROFILE_UPDATED et toutes les actions sont acceptées, sans perte de données")
    void everyAuditActionIsAcceptedAfterConversion() throws SQLException {
        createLegacySchema();
        insertAudit("LOGIN_SUCCESS");
        SQLException rejected = assertThrows(SQLException.class, () -> insertAudit("PROFILE_UPDATED"));
        assertEquals("22030", rejected.getSQLState());

        List<String> converted = EnumColumnMigration.widenEnumColumns(connection).stream()
                .map(EnumColumn::toString)
                .sorted()
                .toList();

        assertEquals(List.of("AUDIT_LOGS.ACTION", "AUDIT_LOGS.OUTCOME", "KYC_DOCUMENTS.STATUS", "KYC_DOCUMENTS.TYPE"),
                converted);
        assertEquals("LOGIN_SUCCESS", queryString("SELECT action FROM audit_logs"));
        assertEquals("APPROVED", queryString("SELECT status FROM kyc_documents"));
        assertEquals(255, characterMaximumLength("AUDIT_LOGS", "ACTION"));
        assertEquals(500, characterMaximumLength("AUDIT_LOGS", "DETAIL"), "colonne non enum inchangée");

        for (AuditLog.Action action : AuditLog.Action.values()) {
            insertAudit(action.name());
        }
        assertEquals(1 + AuditLog.Action.values().length, queryLong("SELECT COUNT(*) FROM audit_logs"));
    }

    @Test
    @DisplayName("la conversion conserve NOT NULL et les index")
    void keepsNotNullAndIndexes() throws SQLException {
        createLegacySchema();

        EnumColumnMigration.widenEnumColumns(connection);

        SQLException missingType = assertThrows(SQLException.class, () -> execute(
                "INSERT INTO kyc_documents (owner_user_id, type, status) VALUES (2, NULL, 'PENDING')"));
        assertEquals("23502", missingType.getSQLState());
        assertEquals(1, queryLong(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE INDEX_NAME = 'IDX_KYC_STATUS'"));
    }

    @Test
    @DisplayName("détecte les colonnes ENUM avec leur nullabilité")
    void findsEnumColumnsWithNullability() throws SQLException {
        createLegacySchema();

        List<EnumColumn> found = EnumColumnMigration.findEnumColumns(connection, Dialect.H2);

        assertEquals(4, found.size());
        assertTrue(found.contains(new EnumColumn("AUDIT_LOGS", "ACTION", true)));
        assertTrue(found.contains(new EnumColumn("KYC_DOCUMENTS", "TYPE", false)));
    }

    @Test
    @DisplayName("la conversion est idempotente")
    void secondRunConvertsNothing() throws SQLException {
        createLegacySchema();

        assertFalse(EnumColumnMigration.widenEnumColumns(connection).isEmpty());
        assertTrue(EnumColumnMigration.widenEnumColumns(connection).isEmpty());
    }

    @Test
    @DisplayName("schéma sans colonne ENUM : aucune modification")
    void schemaWithoutEnumColumnIsUntouched() throws SQLException {
        execute("CREATE TABLE audit_logs (id BIGINT PRIMARY KEY, action VARCHAR(40) NOT NULL)");

        assertTrue(EnumColumnMigration.widenEnumColumns(connection).isEmpty());
        assertEquals(40, characterMaximumLength("AUDIT_LOGS", "ACTION"));
    }

    @Test
    @DisplayName("instructions de conversion : syntaxe H2 et MySQL, nullabilité préservée")
    void alterStatementsPerDialect() {
        EnumColumn nullable = new EnumColumn("audit_logs", "action", true);
        EnumColumn required = new EnumColumn("kyc_documents", "type", false);

        assertEquals("ALTER TABLE \"audit_logs\" ALTER COLUMN \"action\" SET DATA TYPE VARCHAR(255)",
                EnumColumnMigration.alterStatement(Dialect.H2, "\"", nullable));
        assertEquals("ALTER TABLE `audit_logs` MODIFY COLUMN `action` VARCHAR(255) NULL",
                EnumColumnMigration.alterStatement(Dialect.MYSQL, "`", nullable));
        assertEquals("ALTER TABLE `kyc_documents` MODIFY COLUMN `type` VARCHAR(255) NOT NULL",
                EnumColumnMigration.alterStatement(Dialect.MYSQL, "`", required));
    }

    @Test
    @DisplayName("détection du dialecte : H2, MySQL, MariaDB ; autres bases ignorées")
    void detectsSupportedDialects() throws SQLException {
        assertEquals(Optional.of(Dialect.H2), Dialect.detect(connection.getMetaData()));
        assertEquals(Optional.of(Dialect.MYSQL), Dialect.detect(metaDataWithProduct("MySQL")));
        assertEquals(Optional.of(Dialect.MYSQL), Dialect.detect(metaDataWithProduct("MariaDB")));
        assertEquals(Optional.empty(), Dialect.detect(metaDataWithProduct("PostgreSQL")));
    }

    /** Schéma tel que Hibernate le crée avec {@code ddl-auto=update} avant le module 2. */
    private void createLegacySchema() throws SQLException {
        String actions = LEGACY_AUDIT_ACTIONS.stream()
                .map(action -> "'" + action + "'")
                .collect(Collectors.joining(", "));
        execute("CREATE TABLE audit_logs (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, "
                + "action ENUM(" + actions + "), created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL, "
                + "detail VARCHAR(500), ip_address VARCHAR(64), outcome ENUM('FAILURE', 'SUCCESS'), "
                + "resource_id VARCHAR(64), resource_type VARCHAR(60), user_id BIGINT)");
        execute("CREATE TABLE kyc_documents (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, "
                + "owner_user_id BIGINT, type ENUM('PASSPORT', 'ID_CARD') NOT NULL, "
                + "status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING' NOT NULL)");
        execute("CREATE INDEX idx_kyc_status ON kyc_documents (status)");
        execute("INSERT INTO kyc_documents (owner_user_id, type, status) VALUES (1, 'PASSPORT', 'APPROVED')");
    }

    private void insertAudit(String action) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO audit_logs (action, created_at, outcome, user_id) "
                        + "VALUES (?, CURRENT_TIMESTAMP, 'SUCCESS', 1)")) {
            statement.setString(1, action);
            statement.executeUpdate();
        }
    }

    private void execute(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String queryString(String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            assertTrue(rs.next(), sql);
            return rs.getString(1);
        }
    }

    private long queryLong(String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            assertTrue(rs.next(), sql);
            return rs.getLong(1);
        }
    }

    private int characterMaximumLength(String table, String column) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet rs = statement.executeQuery()) {
                assertTrue(rs.next(), table + "." + column);
                return rs.getInt(1);
            }
        }
    }

    /** Métadonnées minimales renvoyant seulement le nom du produit (MySQL n'est pas disponible en test). */
    private static DatabaseMetaData metaDataWithProduct(String product) {
        return (DatabaseMetaData) Proxy.newProxyInstance(
                EnumColumnMigrationTest.class.getClassLoader(),
                new Class<?>[] {DatabaseMetaData.class},
                (proxy, method, args) -> "getDatabaseProductName".equals(method.getName()) ? product : null);
    }
}
