package com.prdv.rdv.profile.domain.model.health;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Service de domaine : evaluation des alertes automatiques de sante.
 *
 * <p>Applique une liste de {@link MetricAlertRule} (Specification) a une
 * mesure. Le moteur est injecte avec ses regles : les remplacer par des
 * regles par patient, par pathologie, ou par un modele d'IA ne modifie ni ce
 * service ni les cas d'usage (Open/Closed + Dependency Inversion).
 *
 * <p>Si plusieurs regles correspondent, la plus grave est retenue pour eviter
 * le bruit (une seule alerte actionnable par mesure).
 */
public class HealthAlertEvaluator {

    private static final Comparator<MetricAlertRule> BY_SEVERITY =
            Comparator.comparingInt(rule -> rule.severity().ordinal());

    private final List<MetricAlertRule> rules;

    public HealthAlertEvaluator() {
        this(MetricAlertRule.defaultRules());
    }

    public HealthAlertEvaluator(List<MetricAlertRule> rules) {
        this.rules = rules == null ? List.of() : List.copyOf(rules);
    }

    /** Regles applicables a un type de mesure (utilise pour exposer le referentiel de seuils). */
    public List<MetricAlertRule> rulesFor(HealthMetric.MetricType type) {
        return rules.stream().filter(rule -> rule.metricType() == type).toList();
    }

    public List<MetricAlertRule> rules() {
        return rules;
    }

    /** Alertes declenchees par une mesure (au plus une, la plus grave). */
    public Optional<HealthAlert> evaluate(HealthMetric metric, Long userId, Clock clock) {
        return matchingRule(metric).map(rule -> HealthAlert.raise(userId, rule, metric, clock));
    }

    /** Evaluation par lot : pratique pour l'import depuis un objet connecte. */
    public List<HealthAlert> evaluateAll(List<HealthMetric> metrics, Long userId, Clock clock) {
        List<HealthAlert> alerts = new ArrayList<>();
        for (HealthMetric metric : metrics) {
            evaluate(metric, userId, clock).ifPresent(alerts::add);
        }
        return alerts;
    }

    private Optional<MetricAlertRule> matchingRule(HealthMetric metric) {
        return rules.stream()
                .filter(rule -> rule.isSatisfiedBy(metric))
                .max(BY_SEVERITY);
    }
}
