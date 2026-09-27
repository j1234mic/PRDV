package com.prdv.rdv.profile.config;

import com.prdv.rdv.profile.domain.model.health.HealthAlertEvaluator;
import com.prdv.rdv.profile.domain.model.health.MetricAlertRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Instanciation des services de domaine du module profils.
 *
 * <p>Le domaine reste pur (aucune annotation Spring sur ses classes) : c'est
 * la configuration qui fournit les beans, ce qui permet de tester les regles
 * metier sans conteneur.
 */
@Configuration
public class ProfileDomainConfig {

    /**
     * Moteur d'alertes de sante avec les seuils par defaut.
     * Remplacer ce bean (seuils par patient, par pathologie, ou modele d'IA)
     * ne modifie aucun cas d'usage.
     */
    @Bean
    public HealthAlertEvaluator healthAlertEvaluator() {
        return new HealthAlertEvaluator(defaultRules());
    }

    /** Point d'extension : regles d'alerte personnalisees. */
    protected List<MetricAlertRule> defaultRules() {
        return MetricAlertRule.defaultRules();
    }
}
