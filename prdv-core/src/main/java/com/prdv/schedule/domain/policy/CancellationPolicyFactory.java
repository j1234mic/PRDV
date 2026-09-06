package com.prdv.schedule.domain.policy;

import java.time.Duration;

/**
 * Pattern FACTORY : materialise le reglage choisi par le medecin en objet politique.
 * Point d'extension unique si on ajoute un mode (ex. PENALITE_PROGRESSIVE).
 */
public final class CancellationPolicyFactory {

    private static final int FEE_24H_CENTS = 1000; // 10 EUR
    private static final int FEE_48H_CENTS = 2000; // 20 EUR

    private CancellationPolicyFactory() {
    }

    public static CancellationPolicy forMode(CancellationMode mode) {
        return switch (mode) {
            case FLEXIBLE -> new HourWindowCancellationPolicy(Duration.ZERO, 0);
            case STANDARD_24H -> new HourWindowCancellationPolicy(Duration.ofHours(24), FEE_24H_CENTS);
            case STRICT_48H -> new HourWindowCancellationPolicy(Duration.ofHours(48), FEE_48H_CENTS);
        };
    }
}
