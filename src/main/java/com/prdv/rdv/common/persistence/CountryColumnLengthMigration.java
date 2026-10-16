package com.prdv.rdv.common.persistence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * Elargissement des colonnes {@code VARCHAR} trop courtes pour les pays.
 *
 * <p>Le schéma initial (ou une base créée avant la correction) déclarait
 * {@code country}, {@code birth_country} et {@code nationality} en
 * {@code VARCHAR(3)}, alors que les DTO et la documentation acceptent des
 * libellés complets (« France », « Française »). Avec {@code ddl-auto=update},
 * Hibernate ne modifie jamais une colonne existante : la valeur est alors
 * refusée par la base ({@code DataIntegrityViolationException} -> HTTP 409
 * {@code DATA_CONFLICT}).
 *
 * <p>Ce composant, exécuté après la création de l'{@code EntityManagerFactory},
 * élargit ces colonnes à {@value #TARGET_LENGTH} caractères quand elles sont
 * trop courtes. Il est idempotent : une colonne déjà à la bonne longueur (ou
 * absente) n'est pas touchée.
 */
@Component
@DependsOn("entityManagerFactory")
public class CountryColumnLengthMigration implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(CountryColumnLengthMigration.class);

    static final int TARGET_LENGTH = 100;

    private static final List<String[]> TARGET_COLUMNS = List.of(
            new String[]{"PROFILE_PRACTICE_LOCATIONS", "COUNTRY"},
            new String[]{"PROFILE_PATIENT_IDENTITIES", "BIRTH_COUNTRY"},
            new String[]{"PROFILE_PATIENT_IDENTITIES", "NATIONALITY"});

    private static final java.util.Set<String> SCHEMA_MANAGED_MODES =
            java.util.Set.of("update", "create", "create-drop");

    private final DataSource dataSource;
    private final String ddlAuto;

    public CountryColumnLengthMigration(DataSource dataSource,
                                        @Value("${spring.jpa.hibernate.ddl-auto:none}") String ddlAuto) {
        this.dataSource = dataSource;
        this.ddlAuto = ddlAuto;
    }

    @Override
    public void afterPropertiesSet() {
        if (!SCHEMA_MANAGED_MODES.contains(ddlAuto)) {
            log.debug("ddl-auto={} : schema non gere, aucune elargissement de colonne pays", ddlAuto);
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            EnumColumnMigration.Dialect dialect = EnumColumnMigration.Dialect.detect(connection.getMetaData())
                    .orElse(null);
            if (dialect == null) {
                log.debug("Dialecte non supporte : aucune migration des colonnes pays");
                return;
            }
            String schema = dialect == EnumColumnMigration.Dialect.H2
                    ? connection.getSchema() : connection.getCatalog();
            String quote = connection.getMetaData().getIdentifierQuoteString();
            List<ColumnRef> refs = findShortColumns(connection, schema);
            if (refs.isEmpty()) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                for (ColumnRef ref : refs) {
                    statement.execute(alterStatement(dialect, quote, ref));
                    log.warn("Colonne elargie : {}.{} ({} -> {})",
                            ref.tableName(), ref.columnName(), ref.length(), TARGET_LENGTH);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Elargissement des colonnes pays impossible : " + e.getMessage(), e);
        }
    }

    private List<ColumnRef> findShortColumns(Connection connection, String schema) throws SQLException {
        List<ColumnRef> result = new ArrayList<>();
        for (String[] target : TARGET_COLUMNS) {
            String sql = "SELECT TABLE_NAME, COLUMN_NAME, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE "
                    + "FROM INFORMATION_SCHEMA.COLUMNS "
                    + "WHERE TABLE_SCHEMA = ? AND UPPER(TABLE_NAME) = ? AND UPPER(COLUMN_NAME) = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, schema);
                statement.setString(2, target[0]);
                statement.setString(3, target[1]);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        int current = rs.getInt("CHARACTER_MAXIMUM_LENGTH");
                        if (rs.wasNull()) {
                            continue;
                        }
                        if (current >= TARGET_LENGTH) {
                            continue;
                        }
                        result.add(new ColumnRef(
                                rs.getString("TABLE_NAME"),
                                rs.getString("COLUMN_NAME"),
                                current,
                                "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE"))));
                    }
                }
            }
        }
        return result;
    }

    private String alterStatement(EnumColumnMigration.Dialect dialect, String quote, ColumnRef ref) {
        String table = quote(ref.tableName(), quote);
        String column = quote(ref.columnName(), quote);
        String varchar = "VARCHAR(" + TARGET_LENGTH + ")";
        return switch (dialect) {
            case H2 -> "ALTER TABLE " + table + " ALTER COLUMN " + column + " SET DATA TYPE " + varchar;
            case MYSQL -> "ALTER TABLE " + table + " MODIFY COLUMN " + column + " " + varchar
                    + (ref.nullable() ? " NULL" : " NOT NULL");
        };
    }

    private static String quote(String identifier, String quote) {
        return quote + identifier.replace(quote, quote + quote) + quote;
    }

    /** Colonne a elargir, avec sa nullabilite. */
    record ColumnRef(String tableName, String columnName, int length, boolean nullable) {
    }
}
