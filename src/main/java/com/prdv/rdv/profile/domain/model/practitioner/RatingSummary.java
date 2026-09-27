package com.prdv.rdv.profile.domain.model.practitioner;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Value Object : synthese des avis d'un praticien (note moyenne, nombre
 * d'avis, repartition par etoile). Calculee a la volee a partir des avis
 * visibles : aucune donnee derivee n'est stockee (pas de desynchronisation
 * possible).
 */
public record RatingSummary(int reviewCount, BigDecimal averageScore, Map<Integer, Integer> distribution) {

    public static final int MAX_STARS = 5;

    public RatingSummary {
        distribution = distribution == null ? emptyDistribution() : Map.copyOf(distribution);
        averageScore = averageScore == null ? BigDecimal.ZERO : averageScore;
    }

    public static RatingSummary empty() {
        return new RatingSummary(0, BigDecimal.ZERO, emptyDistribution());
    }

    public static RatingSummary of(List<PractitionerRating> ratings) {
        Map<Integer, Integer> distribution = emptyDistribution();
        if (ratings == null || ratings.isEmpty()) {
            return empty();
        }
        int total = 0;
        for (PractitionerRating rating : ratings) {
            distribution.merge(rating.getScore(), 1, Integer::sum);
            total += rating.getScore();
        }
        BigDecimal average = BigDecimal.valueOf(total)
                .divide(BigDecimal.valueOf(ratings.size()), 2, RoundingMode.HALF_UP);
        return new RatingSummary(ratings.size(), average, distribution);
    }

    public boolean isEmpty() {
        return reviewCount == 0;
    }

    private static Map<Integer, Integer> emptyDistribution() {
        Map<Integer, Integer> distribution = new LinkedHashMap<>();
        for (int star = MAX_STARS; star >= 1; star--) {
            distribution.put(star, 0);
        }
        return distribution;
    }
}
