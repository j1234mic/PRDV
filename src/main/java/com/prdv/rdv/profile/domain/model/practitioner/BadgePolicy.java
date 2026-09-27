package com.prdv.rdv.profile.domain.model.practitioner;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de domaine : politique d'attribution des badges praticiens.
 *
 * <p>Les regles sont explicites, testables et centralisees ici (plutot que
 * dispersees dans les services applicatifs) :
 * <ul>
 *     <li><b>VERIFIE</b> : identite, diplomes, assurance RC pro et RIB verifies
 *     (donnees issues du module IAM, transmises via un port) ;</li>
 *     <li><b>POPULAIRE</b> : au moins {@value #POPULAR_MIN_REVIEWS} avis et une
 *     moyenne d'au moins {@value #POPULAR_MIN_AVERAGE_STRING} / 5 ;</li>
 *     <li><b>NOUVEAU</b> : compte ouvert depuis moins de {@value #NEW_BADGE_DAYS}
 *     jours (le badge expire ensuite automatiquement).</li>
 * </ul>
 */
public final class BadgePolicy {

    public static final int NEW_BADGE_DAYS = 90;
    public static final int POPULAR_MIN_REVIEWS = 20;
    public static final BigDecimal POPULAR_MIN_AVERAGE = BigDecimal.valueOf(4.5);
    public static final String POPULAR_MIN_AVERAGE_STRING = "4,5";

    private BadgePolicy() {
    }

    /** Etat des verifications reglementaires, fourni par le contexte IAM. */
    public record VerificationState(boolean identityVerified,
                                    boolean diplomaVerified,
                                    boolean professionalInsuranceVerified,
                                    boolean bankAccountVerified) {

        public boolean isFullyVerified() {
            return identityVerified && diplomaVerified && professionalInsuranceVerified && bankAccountVerified;
        }

        public static VerificationState none() {
            return new VerificationState(false, false, false, false);
        }
    }

    /** Recalcule la liste des badges merités a un instant donne. */
    public static List<Badge> evaluate(PractitionerDossier dossier,
                                       RatingSummary ratingSummary,
                                       VerificationState verification,
                                       Instant accountCreatedAt,
                                       Clock clock) {
        List<Badge> badges = new ArrayList<>();
        Instant now = clock.instant();

        if (verification != null && verification.isFullyVerified()) {
            badges.add(new Badge(Badge.BadgeType.VERIFIED, Badge.BadgeSource.AUTOMATIC,
                    "Identite, diplomes, assurance et RIB verifies", now, null));
        }

        RatingSummary summary = ratingSummary == null ? RatingSummary.empty() : ratingSummary;
        if (summary.reviewCount() >= POPULAR_MIN_REVIEWS
                && summary.averageScore().compareTo(POPULAR_MIN_AVERAGE) >= 0) {
            badges.add(new Badge(Badge.BadgeType.POPULAR, Badge.BadgeSource.AUTOMATIC,
                    summary.reviewCount() + " avis avec une moyenne de " + summary.averageScore() + "/5",
                    now, null));
        }

        if (accountCreatedAt != null
                && Duration.between(accountCreatedAt, now).toDays() < NEW_BADGE_DAYS) {
            badges.add(new Badge(Badge.BadgeType.NEW, Badge.BadgeSource.AUTOMATIC,
                    "Praticien inscrit depuis moins de " + NEW_BADGE_DAYS + " jours",
                    now, accountCreatedAt.plus(Duration.ofDays(NEW_BADGE_DAYS))));
        }

        if (dossier != null && !dossier.isPublishable()) {
            // Un dossier incomplet ne peut beneficier d'aucune mise en avant.
            return List.of();
        }
        return List.copyOf(badges);
    }
}
