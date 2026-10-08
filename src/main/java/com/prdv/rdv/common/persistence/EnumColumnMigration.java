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

/**
 * Conversion des colonnes {@code ENUM} natives du schéma en {@code VARCHAR}.
 *
 * <p>Hibernate crée, pour chaque attribut {@code @Enumerated(EnumType.STRING)}, une colonne
 * {@code ENUM} native sur H2 et MySQL. Avec {@code ddl-auto=update}, Hibernate ne modifie jamais
 * une colonne existante : une constante ajoutée à l'énumération Java n'est alors jamais acceptée
 * par la base (H2 : {@code 22030 « Valeur non permise pour la colonne »}). C'est ce qui bloquait
 * l'insertion de {@code PROFILE_UPDATED} dans {@code audit_logs}, table créée avant l'ajout des
 * actions du module 2.
 *
 * <p>La conversion en {@code VARCHAR({@value #TARGET_LENGTH})} lève cette rigidité pour toutes les
 * colonnes concernées ; la validation des valeurs reste assurée par l'énumération Java. Elle ne perd
 * aucune donnée : les valeurs existantes, la contrainte {@code NOT NULL} et les index sont conservés.
 * Hibernate ne pose pas de valeur par défaut sur ces colonnes, que {@code MODIFY} (MySQL) ne
 * reprendrait pas. Elle est idempotente : une colonne déjà convertie n'est plus détectée.
 */
final class EnumColumnMigration {

    /** Longueur cible : celle que Hibernate donne aux chaînes sans {@code length} explicite. */
    static final int TARGET_LENGTH = 255;

    private EnumColumnMigration() {
    }

    /** Colonne {@code ENUM} détectée, avec sa nullabilité (à conserver lors de la conversion). */
    record EnumColumn(String tableName, String columnName, boolean nullable) {

        @Override
        public String toString() {
            return tableName + "." + columnName;
        }
    }

    /** Dialectes pris en charge : la syntaxe d'{@code ALTER TABLE} diffère pour chacun. */
    enum Dialect {
        H2,
        MYSQL;

        /** Détecte le dialecte ; vide si la base n'est ni H2 ni MySQL/MariaDB. */
        static Optional<Dialect> detect(DatabaseMetaData metaData) throws SQLException {
            String product = metaData.getDatabaseProductName();
            if (product == null) {
                return Optional.empty();
            }
            String normalized = product.toLowerCase(Locale.ROOT);
            if (normalized.equals("h2")) {
                return Optional.of(H2);
            }
            if (normalized.contains("mysql") || normalized.contains("mariadb")) {
                return Optional.of(MYSQL);
            }
            return Optional.empty();
        }
    }

    /**
     * Convertit en {@code VARCHAR} toutes les colonnes {@code ENUM} du schéma courant.
     *
     * @return les colonnes converties ; liste vide si rien à faire
     * @throws SQLException si une instruction de conversion échoue
     */
    static List<EnumColumn> widenEnumColumns(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        Optional<Dialect> dialect = Dialect.detect(metaData);
        if (dialect.isEmpty()) {
            return List.of();
        }
        List<EnumColumn> columns = findEnumColumns(connection, dialect.get());
        if (columns.isEmpty()) {
            return List.of();
        }
        String quote = metaData.getIdentifierQuoteString();
        try (Statement statement = connection.createStatement()) {
            for (EnumColumn column : columns) {
                statement.execute(alterStatement(dialect.get(), quote, column));
            }
        }
        return columns;
    }

    /**
     * Liste les colonnes {@code ENUM} du schéma courant : {@code PUBLIC} pour H2, base courante
     * pour MySQL.
     */
    static List<EnumColumn> findEnumColumns(Connection connection, Dialect dialect) throws SQLException {
        String schema = dialect == Dialect.H2 ? connection.getSchema() : connection.getCatalog();
        String sql = "SELECT TABLE_NAME, COLUMN_NAME, IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = ? AND UPPER(DATA_TYPE) = 'ENUM' "
                + "ORDER BY TABLE_NAME, ORDINAL_POSITION";
        List<EnumColumn> columns = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    columns.add(new EnumColumn(
                            rs.getString("TABLE_NAME"),
                            rs.getString("COLUMN_NAME"),
                            "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE"))));
                }
            }
        }
        return columns;
    }

    /** Instruction de conversion d'une colonne, selon le dialecte. */
    static String alterStatement(Dialect dialect, String quote, EnumColumn column) {
        String table = quote(column.tableName(), quote);
        String name = quote(column.columnName(), quote);
        String varchar = "VARCHAR(" + TARGET_LENGTH + ")";
        return switch (dialect) {
            // SET DATA TYPE conserve NOT NULL, DEFAULT et les index.
            case H2 -> "ALTER TABLE " + table + " ALTER COLUMN " + name + " SET DATA TYPE " + varchar;
            // MODIFY remplace la définition complète : la nullabilité doit être re-précisée.
            case MYSQL -> "ALTER TABLE " + table + " MODIFY COLUMN " + name + " " + varchar
                    + (column.nullable() ? " NULL" : " NOT NULL");
        };
    }

    private static String quote(String identifier, String quote) {
        return quote + identifier.replace(quote, quote + quote) + quote;
    }
}
