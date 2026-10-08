package com.prdv.rdv.common.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * Amorçage du schéma : convertit au démarrage les colonnes {@code ENUM} natives en {@code VARCHAR}.
 *
 * <p>S'exécute après la création de l'{@code EntityManagerFactory}, donc après le {@code ddl-auto}
 * de Hibernate, et avant le démarrage du serveur HTTP et des {@code ApplicationRunner} (dont
 * {@code DataSeeder}). Voir {@link EnumColumnMigration} pour le détail de la conversion.
 *
 * <p>N'agit que si Hibernate gère le schéma ({@code update}, {@code create}, {@code create-drop}) :
 * avec {@code none} ou {@code validate}, l'application ne modifie jamais le schéma.
 */
@Component
@DependsOn("entityManagerFactory")
public class EnumColumnReconciler implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(EnumColumnReconciler.class);

    private static final Set<String> SCHEMA_MANAGED_MODES = Set.of("update", "create", "create-drop");

    private final DataSource dataSource;
    private final String ddlAuto;

    public EnumColumnReconciler(DataSource dataSource,
                                @Value("${spring.jpa.hibernate.ddl-auto:none}") String ddlAuto) {
        this.dataSource = dataSource;
        this.ddlAuto = ddlAuto;
    }

    @Override
    public void afterPropertiesSet() {
        if (!SCHEMA_MANAGED_MODES.contains(ddlAuto)) {
            log.debug("ddl-auto={} : schema non gere par Hibernate, aucune conversion ENUM", ddlAuto);
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            List<EnumColumnMigration.EnumColumn> converted = EnumColumnMigration.widenEnumColumns(connection);
            if (!converted.isEmpty()) {
                log.warn("Colonnes ENUM converties en VARCHAR({}) : {}. ddl-auto=update ne modifie pas les "
                        + "colonnes existantes ; sans cette conversion, les nouvelles valeurs de l'enumeration "
                        + "Java seraient refusees par la base.",
                        EnumColumnMigration.TARGET_LENGTH, converted);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Conversion des colonnes ENUM en VARCHAR impossible : " + e.getMessage(), e);
        }
    }
}
